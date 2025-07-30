package net.chamman.moonnight.global.security.fillter;

import java.io.IOException;
import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import com.nimbusds.oauth2.sdk.util.StringUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.sign.SignService;
import net.chamman.moonnight.auth.sign.log.SignLogService;
import net.chamman.moonnight.auth.token.JwtProvider;
import net.chamman.moonnight.auth.token.TokenAuthenticator;
import net.chamman.moonnight.auth.token.TokenProvider;
import net.chamman.moonnight.auth.token.TokenProvider.TokenType;
import net.chamman.moonnight.global.context.RequestContext;
import net.chamman.moonnight.global.context.RequestContextHolder;
import net.chamman.moonnight.global.exception.jwt.TimeOutJwtException;
import net.chamman.moonnight.global.security.principal.CustomUserDetails;
import net.chamman.moonnight.global.util.ClientIpExtractor;
import net.chamman.moonnight.global.util.LogMaskingUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil.MaskLevel;

@Slf4j
@Component
public class SilentAuthenticationFilter extends AbstractAccessTokenFilter<CustomUserDetails>{

	public SilentAuthenticationFilter(JwtProvider jwtProvider, TokenProvider tokenProvider,
			SignLogService signLogService, SignService signService, TokenAuthenticator tokenAuthenticator) {
		super(jwtProvider, tokenProvider, signLogService, signService, tokenAuthenticator);
	}
	
	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
			FilterChain filterChain) throws ServletException, IOException {
		
		// ClientIp
		String clientIp = ClientIpExtractor.extractClientIp(req);
		
		// Mobile Or Web
		String clientType = req.getHeader("X-Client-Type");
		boolean isMobileApp = clientType != null && clientType.contains("mobile");
		
		log.debug("* [{}] SilentAuthenticationFilter 실행 - clientIp: [{}], isMobileApp: [{}]", req.getRequestURI(), clientIp, isMobileApp);

		RequestContext requestContext = new RequestContext(clientIp, isMobileApp);
		RequestContextHolder.setContext(requestContext);
		
		// AccessToken
		String accessToken = getAccessToken(isMobileApp, req);
		String refreshToken = getRefreshToken(isMobileApp, req);
		
		log.debug("* AccessToken: [{}], RefreshToken: [{}]", LogMaskingUtil.maskToken(accessToken, MaskLevel.MEDIUM), LogMaskingUtil.maskToken(refreshToken, MaskLevel.MEDIUM));

		// 1. 토큰 null 체크
		if(StringUtils.isBlank(accessToken) || StringUtils.isBlank(refreshToken)) {
			log.warn("* 토큰 누락 또는 빈 값.");
			initTokenToCookie(res);
			filterChain.doFilter(req, res);
			return;
		}
		
//		 2. 블랙리스트 확인
		String value = tokenProvider.getBlackListValue(accessToken);
		log.debug("* 블랙리스트 조회 결과 value: [{}]",value);
		
//		 2-1. 로그아웃한 토큰
		if(Objects.equals(value, "SIGNOUT")) {
			initTokenToCookie(res);
			filterChain.doFilter(req, res);
			return;
//		2-2. 유저 정보 업데이트된 토큰
			
		} else if(Objects.equals(value, "UPDATE")) {
			try {
				refresh(refreshToken, clientIp, res, isMobileApp);
				tokenProvider.removeToken(TokenType.JWT_BLACKLIST, accessToken);
				filterChain.doFilter(req, res);
				return;
			} catch (Exception refreshEx) {
				log.error("* AccessToken UPDATE 인해 Refresh 중 익셉션 발생.",refreshEx);
				initTokenToCookie(res);
				filterChain.doFilter(req, res);
				return;
			}
		}
		
		// 3. 토큰 확인 및 Set
		try {
			setAuthentication(accessToken);
			
			filterChain.doFilter(req, res);
			return;
			
		// 4. Access Token 만료.
		} catch (TimeOutJwtException e) {
			try {
				log.debug("* AccessToken 만료 확인.");
				refresh(refreshToken, clientIp, res, isMobileApp);
				filterChain.doFilter(req, res);
				return;

			} catch (Exception refreshEx) {
				log.error("* AccessToken 만료 인해 Refresh 중 익셉션 발생.", refreshEx);
				initTokenToCookie(res);
				filterChain.doFilter(req, res);
				return;
			}
		} catch (Exception e) {
			log.error("* AccessToken validate 실패 또는 UserDetails 생성 중 실패.", e);
			initTokenToCookie(res);
			filterChain.doFilter(req, res);
			return;
		}
	}
	
	@Override
	protected CustomUserDetails buildUserDetails(String accessToken) {
		log.debug("* SilentAuthenticationFilter.buildUserDetails 실행.");
		
		return tokenAuthenticator.authenticateAccessToken(accessToken);
	}
	
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		String uri = request.getRequestURI();
		
		// Spring의 AntPathMatcher를 사용
		AntPathMatcher matcher = new AntPathMatcher();
		
		return matcher.match("/css/**", uri)
				|| matcher.match("/js/**", uri)
				|| matcher.match("/images/**", uri)
				|| matcher.match("/swagger-ui/**", uri)
				|| matcher.match("/api-docs/**", uri)
				|| matcher.match("/v3/api-docs/**", uri)
				|| matcher.match("/openapi.yaml", uri)
				|| matcher.match("/.well-known/**", uri)
				|| matcher.match("/favicon.ico", uri)
				|| matcher.match("/jpg/**", uri)
				|| matcher.match("/api/*/private/**", uri);
	}
	
	
}
