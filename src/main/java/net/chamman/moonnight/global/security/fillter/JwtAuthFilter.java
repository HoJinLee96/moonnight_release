package net.chamman.moonnight.global.security.fillter;

import java.io.IOException;
import java.time.Duration;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.sign.SignService;
import net.chamman.moonnight.auth.sign.log.SignLogService;
import net.chamman.moonnight.auth.token.JwtProvider;
import net.chamman.moonnight.auth.token.TokenAuthenticator;
import net.chamman.moonnight.auth.token.TokenProvider;
import net.chamman.moonnight.global.context.RequestContext;
import net.chamman.moonnight.global.context.RequestContextHolder;
import net.chamman.moonnight.global.exception.jwt.TimeOutJwtException;
import net.chamman.moonnight.global.security.principal.AuthDetails;
import net.chamman.moonnight.global.util.ClientIpExtractor;
import net.chamman.moonnight.global.util.CookieUtil;

@Component
@Slf4j
public class JwtAuthFilter extends AbstractAccessTokenFilter<AuthDetails> {

	public JwtAuthFilter(JwtProvider jwtProvider, TokenProvider tokenProvider, SignLogService signLogService,
			SignService signService, TokenAuthenticator tokenAuthenticator) {
		super(jwtProvider, tokenProvider, signLogService, signService, tokenAuthenticator);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain)
			throws ServletException, IOException {
		log.debug("* JwtAuthFilter.doFilterInternal 실행.");

		// ClientIp
		String clientIp = ClientIpExtractor.extractClientIp(req);

		// Mobile Or Web
		String clientType = req.getHeader("X-Client-Type");
		boolean isMobileApp = clientType != null && clientType.contains("mobile");
		
		RequestContext requestContext = new RequestContext(clientIp, isMobileApp);
		RequestContextHolder.setContext(requestContext);

		String authToken = null;
		if (isMobileApp) {
			authToken = req.getHeader("X-Auth-Token");
		} else {
			Cookie cookie = WebUtils.getCookie(req, "X-Auth-Token");
			if (cookie != null) {
				authToken = cookie.getValue();
			}
		}

//		 1. 토큰 null 체크
		log.debug("* JwtAuthFilter 토큰 널 체크.");

		if (authToken == null || authToken.isBlank()) {
			CookieUtil.addCookie(res, "X-Auth-Token", "", Duration.ZERO);
			setErrorResponse(res, 4011, "유효하지 않은 요청 입니다.");
			filterChain.doFilter(req, res);
			return;
		}

//		 2. 블랙리스트 확인
		log.debug("* JwtAuthFilter 토큰 블랙리스트 체크.");

		String value = tokenProvider.getBlackListValue(authToken);
		if (value != null) {
			CookieUtil.addCookie(res, "X-Auth-Token", "", Duration.ZERO);
			setErrorResponse(res, 4012, "유효하지 않은 요청 입니다.");
			filterChain.doFilter(req, res);
			return;
		}

		// 3. 토큰 확인 및 Set
		try {
			setAuthentication(authToken);

			filterChain.doFilter(req, res);
			return;

			// 4. Access Token 만료.
		} catch (TimeOutJwtException e) {
			CookieUtil.addCookie(res, "X-Auth-Token", "", Duration.ZERO);
			setErrorResponse(res, 4012, "유효하지 않은 요청 입니다.");
			filterChain.doFilter(req, res);
			return;
		}

	}

	@Override
	protected AuthDetails buildUserDetails(String accessToken) {
		log.debug("* JwtAuthFilter.buildUserDetails 실행.");
		
		return tokenAuthenticator.authenticateAuthToken(accessToken);
	}

	@Override
	protected void setAuthentication(String authToken) {
		AuthDetails userDetails = buildUserDetails(authToken);
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
				userDetails.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		String uri = request.getRequestURI();
		return !(uri.startsWith("/api/spem/private/auth") 
				|| uri.startsWith("/api/estimate/private/auth")
				|| uri.startsWith("/api/spem/private/auth/**")
				|| uri.startsWith("/api/estimate/private/auth/**"));
	}

}
