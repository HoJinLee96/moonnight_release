package net.chamman.moonnight.global.security.fillter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.util.StringUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.adminSign.AdminSignService;
import net.chamman.moonnight.auth.adminSign.log.AdminSignLogService;
import net.chamman.moonnight.auth.token.JwtProvider;
import net.chamman.moonnight.auth.token.TokenProvider;
import net.chamman.moonnight.auth.token.TokenProvider.TokenType;
import net.chamman.moonnight.global.exception.jwt.TimeOutJwtException;
import net.chamman.moonnight.global.security.principal.TokenAuthenticator;
import net.chamman.moonnight.global.util.ClientIpExtractor;
import net.chamman.moonnight.global.util.CookieUtil;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractAccessTokenFilter<T extends UserDetails> extends OncePerRequestFilter {

	protected final JwtProvider jwtProvider;
	protected final TokenProvider tokenProvider;
	protected final AdminSignLogService adminSignLogService;
	protected final AdminSignService adminSignService;
	protected final TokenAuthenticator tokenAuthenticator;

	protected abstract T buildDetails(String accessToken);

	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain)
			throws ServletException, IOException {
		log.debug("* AbstractAccessTokenFilter.doFilterInternal 실행.");

		// ClientIp
		String clientIp = ClientIpExtractor.extractClientIp(req);

		// Mobile Or Web
		String clientType = req.getHeader("X-Client-Type");
		boolean isMobileApp = clientType != null && clientType.contains("mobile");

		String accessToken = getAccessToken(isMobileApp, req);
		String refreshToken = getRefreshToken(isMobileApp, req);

//		 1. 토큰 null 체크
		if (StringUtils.isBlank(accessToken) || StringUtils.isBlank(refreshToken)) {
			log.warn("* 토큰 누락 또는 빈 값.");
			initTokenToCookie(res);
			setErrorResponse(res, 4011, "유효하지 않은 요청 입니다.");
			return;
		}

//		 2. 블랙리스트 확인
		String value = tokenProvider.getBlackListValue(accessToken);

//		 2-1. 로그아웃한 토큰
		if (Objects.equals(value, "SIGNOUT")) {
			initTokenToCookie(res);
			setErrorResponse(res, 4012, "유효하지 않은 요청 입니다.");
			return;

//		2-2. 유저 정보 업데이트된 토큰
		} else if (Objects.equals(value, "UPDATE")) {
			try {
				refresh(refreshToken, clientIp, res, isMobileApp);
				tokenProvider.removeToken(TokenType.JWT_BLACKLIST, accessToken);
				filterChain.doFilter(req, res);
				return;
			} catch (Exception refreshEx) {
				// 리프레쉬 토큰 통해 SignIn Tokens 재발급 실패
				initTokenToCookie(res);
				setErrorResponse(res, 4010, "유효하지 않은 요청 입니다.");
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
//				리프레쉬 
				refresh(refreshToken, clientIp, res, isMobileApp);
				filterChain.doFilter(req, res);
				return;

			} catch (Exception refreshEx) {
				// 리프레쉬 토큰 통해 SignIn Tokens 재발급 실패
				initTokenToCookie(res);
				setErrorResponse(res, 4010, "유효하지 않은 요청 입니다.");
				return;
			}
			// 그 외 에러
		} catch (Exception e) {
			log.error("* accressToken validate 실패 또는 CustomAdminDetails 생성 중 실패.", e);
			initTokenToCookie(res);
			setErrorResponse(res, 4010, "유효하지 않은 요청 입니다.");
			return;
		}
	}

	protected String getAccessToken(boolean isMobileApp, HttpServletRequest req) {
		if (isMobileApp) {
			return req.getHeader("X-Access-Token");
		} else {
			Cookie cookie = WebUtils.getCookie(req, "X-Access-Token");
			if (cookie != null) {
				return cookie.getValue();
			}
		}
		return null;
	}

	protected String getRefreshToken(boolean isMobileApp, HttpServletRequest req) {
		if (isMobileApp) {
			return req.getHeader("X-Refresh-Token");
		} else {
			Cookie cookie = WebUtils.getCookie(req, "X-Refresh-Token");
			if (cookie != null) {
				return cookie.getValue();
			}
		}
		return null;
	}

	protected void refresh(String refreshToken, String clientIp, HttpServletResponse res, boolean isMobileApp) {
		log.debug("* Refresh 진행.");

		Map<String, String> newTokens = adminSignService.refresh(refreshToken, clientIp);
		// 새로운 토큰 Set Response
		setTokenToResponse(newTokens, res, isMobileApp);
		// 새로운 토큰 통해 SetAuthentication
		setAuthentication(newTokens.get("accessToken"));
	}

	protected void setTokenToResponse(Map<String, String> tokens, HttpServletResponse res, boolean isMobileApp) {
		String accessToken = tokens.get("accessToken");
		String refreshToken = tokens.get("refreshToken");

		if (isMobileApp) {
			res.setHeader("X-Access-Token", accessToken);
			res.setHeader("X-Refresh-Token", refreshToken);
		} else {
			CookieUtil.addCookie(res, "X-Access-Token", accessToken, Duration.ofMinutes(120));
			CookieUtil.addCookie(res, "X-Refresh-Token", refreshToken, Duration.ofDays(14));
		}
	}

	protected void initTokenToCookie(HttpServletResponse res) {
		log.debug("* Cookie AccessToken, RefreshToken 초기화 진행.");

		CookieUtil.addCookie(res, "X-Access-Token", "", Duration.ZERO);
		CookieUtil.addCookie(res, "X-Refresh-Token", "", Duration.ZERO);

	}

	protected void setErrorResponse(HttpServletResponse res, int code, String message) throws IOException {
		res.setStatus(HttpStatus.UNAUTHORIZED.value());
		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");

		Map<String, Object> body = Map.of("code", code, "message", message);
		res.getWriter().write(new ObjectMapper().writeValueAsString(body));
	}

	protected void setAuthentication(String accessToken) {
		T adminDetails = buildDetails(accessToken);
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(adminDetails, null,
				adminDetails.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}
}
