package net.chamman.moonnight.global.security.fillter;

import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.sign.SignService;
import net.chamman.moonnight.auth.sign.log.SignLogService;
import net.chamman.moonnight.auth.token.JwtProvider;
import net.chamman.moonnight.auth.token.TokenAuthenticator;
import net.chamman.moonnight.auth.token.TokenProvider;
import net.chamman.moonnight.global.security.principal.CustomUserDetails;

@Component
@Slf4j
public class JwtFilter extends AbstractAccessTokenFilter<CustomUserDetails> {
	
	public JwtFilter(JwtProvider jwtProvider, TokenProvider tokenProvider, SignLogService signLogService,
			SignService signService, TokenAuthenticator tokenAuthenticator) {
		super(jwtProvider, tokenProvider, signLogService, signService, tokenAuthenticator);
	}
	
	@Override
	protected CustomUserDetails buildUserDetails(String accessToken) {
		log.debug("* JwtFilter.buildUserDetails 실행.");
		
		return tokenAuthenticator.authenticateAccessToken(accessToken);
	}
	
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		AntPathMatcher matcher = new AntPathMatcher();
		String PRIVATE_PATTERN = "/api/**/private/**";
		Set<String> SKIP_PATHS = Set.of(
		        "/api/spem/private/auth",
		        "/api/estimate/private/auth",
		        "/api/spem/private/auth/**",
		        "/api/estimate/private/auth/**"
		);
		
	    String uri = request.getRequestURI();
	    boolean skipResult = false;
	    for (String pattern : SKIP_PATHS) {
	        if (matcher.match(pattern, uri)) {
	        	skipResult = true; 
	        }
	    }
	    // 1) /api/**/private/** 패턴과 일치하고
	    // 2) 예외 리스트에 없으면 → 필터 적용
	    boolean mustFilter = matcher.match(PRIVATE_PATTERN, uri) && !skipResult;

	    // shouldNotFilter가 true면 **필터를 건너뜀**이므로
	    // mustFilter를 뒤집어서 반환
	    return !mustFilter;
	}
	
}
