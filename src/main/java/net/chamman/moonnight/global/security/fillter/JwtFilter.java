package net.chamman.moonnight.global.security.fillter;

import static net.chamman.moonnight.global.exception.HttpStatusCode.JWT_ILLEGAL;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.adminSign.AdminSignService;
import net.chamman.moonnight.auth.adminSign.log.AdminSignLogService;
import net.chamman.moonnight.auth.token.JwtProvider;
import net.chamman.moonnight.auth.token.TokenProvider;
import net.chamman.moonnight.global.exception.jwt.IllegalJwtException;
import net.chamman.moonnight.global.security.principal.CustomAdminDetails;

@Component
@Slf4j
public class JwtFilter extends AbstractAccessTokenFilter<CustomAdminDetails> {

	public JwtFilter(JwtProvider jwtProvider, TokenProvider tokenProvider, AdminSignLogService signLogService,
			AdminSignService signService) {
		super(jwtProvider, tokenProvider, signLogService, signService);
	}

	@Override
	protected CustomAdminDetails buildAdminDetails(String accessToken) {
		log.debug("* JwtFilter.buildAdminDetails 실행.");

		Map<String, Object> claims = jwtProvider.validateAccessToken(accessToken);

		// 복호화 때문에 claims.getSbuject()가 아님.
		Object subjectRaw = claims.get("subject");
		if (subjectRaw == null) {
			throw new IllegalJwtException(JWT_ILLEGAL, "JWT CustomUserDetails 생성중 오류 발생. subject");
		}
		int userId = Integer.parseInt(subjectRaw.toString());
		if (userId == 0) {
			throw new IllegalJwtException(JWT_ILLEGAL, "JWT CustomUserDetails 생성중 오류 발생. subject");
		}

		Object rolesObj = claims.get("roles");
		if (!(rolesObj instanceof List)) {
			throw new IllegalJwtException(JWT_ILLEGAL, "JWT AuthUserDetails 생성중 오류 발생. - roles");
		}
		@SuppressWarnings("unchecked")
		List<String> roles = (List<String>) rolesObj;
		List<GrantedAuthority> authorities = roles.stream().map(SimpleGrantedAuthority::new)
				.collect(Collectors.toList());

		String email = (String) claims.get("email");
		if (email == null || email.isEmpty()) {
			throw new IllegalJwtException(JWT_ILLEGAL, "JWT AuthUserDetails 생성중 오류 발생. - email");
		}
		String name = (String) claims.get("name");
		if (name == null || name.isEmpty()) {
			throw new IllegalJwtException(JWT_ILLEGAL, "JWT AuthUserDetails 생성중 오류 발생. - name");
		}

		return new CustomAdminDetails(userId, email, name, authorities);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		AntPathMatcher matcher = new AntPathMatcher();
		String PRIVATE_PATTERN = "/api/admin/**";
		String uri = request.getRequestURI();
		return !matcher.match(PRIVATE_PATTERN, uri);
	}

}
