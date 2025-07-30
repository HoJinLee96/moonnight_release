package net.chamman.moonnight.auth.oauth;

import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.AesProvider;
import net.chamman.moonnight.auth.oauth.OAuth.OAuthProvider;
import net.chamman.moonnight.auth.sign.SignService;
import net.chamman.moonnight.auth.sign.log.SignLog;
import net.chamman.moonnight.auth.sign.log.SignLog.SignResult;
import net.chamman.moonnight.auth.sign.log.SignLogService;
import net.chamman.moonnight.auth.token.TokenProvider;
import net.chamman.moonnight.auth.token.TokenProvider.TokenType;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.domain.user.User.UserProvider;
import net.chamman.moonnight.domain.user.UserService;
import net.chamman.moonnight.global.exception.CustomException;
import net.chamman.moonnight.global.exception.status.StatusException;
import net.chamman.moonnight.global.exception.user.DuplicationException;
import net.chamman.moonnight.global.util.ClientIpExtractor;
import net.chamman.moonnight.global.util.CookieUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil.MaskLevel;
import net.chamman.moonnight.global.validator.RedirectResolver;

@Component
@Slf4j
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

	private final OAuthService oauthService;
	private final UserService userService;
	private final SignService signService;
	private final TokenProvider tokenProvider;
	private final SignLogService signLogService;
	private final AesProvider aesProvider;
	private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
	private final RedirectResolver redirectResolver;
	private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

	@SuppressWarnings("incomplete-switch")
	@Override
	public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication authentication)
			throws IOException {

		// 1. 정보 추출
		String clientIp = ClientIpExtractor.extractClientIp(req);
		CustomOAuth2User customOAuth2User = extractAuthentication(authentication);
		String email = customOAuth2User.getEmail();
		OAuthProvider oauthProvider = customOAuth2User.getOauthProvider();
		log.debug("* OAuth 인증 성공 핸들러 작동. ClientIp: [{}], [{}]", clientIp, customOAuth2User.toString(MaskLevel.MEDIUM));

		try {
			
			HttpSession session = req.getSession(false);
	        boolean isLinkingFlow = session != null && session.getAttribute("OAUTH_LINK_IN_PROGRESS") != null;

	        // Link 로직
	        if (isLinkingFlow) {
	        	String userIdStr = aesProvider.decrypt((String) session.getAttribute("LINKING_USER_ID"));
	        	int userId = Integer.parseInt(userIdStr);
	            session.removeAttribute("LINKING_USER_ID");
	            session.removeAttribute("OAUTH_LINK_IN_PROGRESS");
	            try {
	            	handleAccountLinking(req, res, authentication, userId, clientIp);
	            } catch(CustomException customException) {
	                redirectStrategy.sendRedirect(req, res, "/my/signInfo?error="+customException.getHttpStatusCode().name());
	            } catch(Exception e) {
	                redirectStrategy.sendRedirect(req, res, "/my/signInfo?error=UNKNOWN_ERROR");
	            }
	            return;
	        }
	        
			// OAuth 유저 조회 (StatusException 발생)
			Optional<OAuth> existOauth = oauthService.getOAuth(oauthProvider, customOAuth2User.getOauthProviderId());

			// 신규 유저
			if (existOauth.isEmpty()) {
				log.debug("* OAuth [{}] 신규 유저. email: [{}]", oauthProvider, LogMaskingUtil.maskEmail(email, MaskLevel.NONE));
				
				// 이메일 중복 검사 (DuplicationException)
				userService.isEmailExistsForRegistration(email);
				// 이메일 탈퇴 유저 검사
				Optional<User> existUser = userService.isEmailDeleted(email);
				String url = existUser.isPresent() ? "/signup/oauth/choice" : "/signup/oauth";

				String signUpOAuthToken = tokenProvider.createToken(customOAuth2User, TokenType.ACCESS_SIGNUP_OAUTH);

				CookieUtil.addCookie(res, "X-Access-SignUp-OAuth-Token", signUpOAuthToken, Duration.ofMinutes(10));
				redirectStrategy.sendRedirect(req, res, url);
				return;
			}

			// 기존 유저
			log.debug("* OAuth {} email: {} 기존 유저.", oauthProvider, LogMaskingUtil.maskEmail(email, MaskLevel.NONE));

			OAuth oauth = existOauth.get();
			User user = oauth.getUser();
			int userId = user.getUserId();
			
			Map<String, String> signJwts = signService.signInOAuthAndCreateJwt(userId, clientIp);

			// 3. 클라이언트별 반환
			CookieUtil.addCookie(res, "X-Access-Token", signJwts.get("accessToken"), Duration.ofMinutes(120)); 
			CookieUtil.addCookie(res, "X-Refresh-Token", signJwts.get("refreshToken"), Duration.ofDays(14));

			String redirect = getRedirect(req, res);

			redirectStrategy.sendRedirect(req, res, redirect);
			
		} catch (StatusException e) {
			// 유저 상태 이상
			log.debug("* OAuth {} 기존 유저 상태 이상. email: {}, {}", oauthProvider, LogMaskingUtil.maskEmail(email, MaskLevel.NONE), e.getHttpStatusCode());

			switch (e.getHttpStatusCode()) {
			case USER_STATUS_STAY -> {
				signLogService.registerSignLog(SignLog.builder()
						.provider(oauthProvider.name())
						.email(email)
						.signResult(SignResult.ACCOUNT_STAY)
						.clientIp(clientIp)
						.build());
				redirectStrategy.sendRedirect(req, res, "/sign/stay");
				return;
			}
			case USER_STATUS_STOP -> {
				signLogService.registerSignLog(SignLog.builder()
						.provider(oauthProvider.name())
						.email(email)
						.signResult(SignResult.ACCOUNT_STOP)
						.clientIp(clientIp)
						.build());
				redirectStrategy.sendRedirect(req, res, "/sign/stop");
				return;
			}
			}
		} catch (DuplicationException e) {
			// 이메일 중복
			log.debug("* OAuth {} 신규 유저 이메일 중복. email: {}, ExistProvider: {}", oauthProvider, LogMaskingUtil.maskEmail(email, MaskLevel.NONE), e.getUserProvider());

			// 중복 유저가 통합회원인 경우
			if (Objects.equals(e.getUserProvider(), UserProvider.LOCAL)) {
				String signUpOAuthToken = tokenProvider.createToken(customOAuth2User, TokenType.ACCESS_SIGNUP_OAUTH);

				CookieUtil.addCookie(res, "X-Link-OAuth-Token", signUpOAuthToken, Duration.ofMinutes(10));
				String redirect = getRedirect(req, res);
				String encodedRedirect = Base64.getEncoder().encodeToString(redirect.getBytes());

				String encodingEmail = Base64.getEncoder().encodeToString(email.getBytes());
	            String redirectUrl = "/signup/exist/local?email=" + encodingEmail+"&redirect="+encodedRedirect;

				redirectStrategy.sendRedirect(req, res, redirectUrl);
				return;

			// 중복 유저가 다른 OAuth 경우
			} else {
				User user = userService.getUserByEmail(email);
	            List<OAuthResponseDto> existingOAuths = oauthService.getOAuthByUser(user);
	            
	            // 2. provider 이름만 추출해서 콤마(,)로 구분된 문자열로 만든다. (예: "KAKAO,NAVER")
	            String providerNames = existingOAuths.stream()
	                                     .map(oauth -> oauth.oauthProvider().name())
	                                     .collect(Collectors.joining(","));
	            String redirectUrl = "/signup/exist/oauth?providers=" + providerNames;

				redirectStrategy.sendRedirect(req, res, redirectUrl);
				return;
			}

		} catch (Exception e) {
			log.error("* OAuth 로그인 중 예외 발생.", e);
			signLogService.registerSignLog(SignLog.builder()
					.provider(oauthProvider.name())
					.email(email)
					.signResult(SignResult.SERVER_ERROR)
					.reason(e.getClass().getSimpleName() + e.getMessage())
					.clientIp(clientIp)
					.build());
			redirectStrategy.sendRedirect(req, res, "/error");
		}
	}

	private OAuthProvider extractProvider(Authentication auth) {
		OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) auth;
		String provider = token.getAuthorizedClientRegistrationId().toUpperCase(); // "naver" or "kakao"
		return OAuthProvider.valueOf(provider);
	}

	private String getRedirect(HttpServletRequest req, HttpServletResponse res) {
		// 1. 쿠키에서 리다이렉트 URL 꺼내기
		Optional<String> redirectUri = CookieUtil
				.getCookie(req, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
				.map(Cookie::getValue);

		// 2. 사용한 쿠키 삭제
		httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(req, res);

		if (redirectUri.isPresent() && !redirectUri.get().isBlank()) {
			String decodedUrl = new String(Base64.getUrlDecoder().decode(redirectUri.get()));
			// 3. 리다이렉트 URL가 내 도메인인지 검증
			if (redirectResolver.isAuthorizedRedirectUri(decodedUrl)) {
				log.debug("* OAUTH로그인 성공 이후 Redirect 값: [{}]", decodedUrl);
				return decodedUrl;
			} else {
				log.info("* 허용되지 않은 Redirect URI 감지: [{}]", decodedUrl);
				return "/";
			}
		}
		return "/";
	}

	private CustomOAuth2User extractAuthentication(Authentication authentication) {

		DefaultOAuth2User oAuth2User = (DefaultOAuth2User) authentication.getPrincipal();
		OAuthProvider oauthProvider = extractProvider(authentication);
		String oauthProviderId = oAuth2User.getName();
		String email = null;
		String name = null;
		if (Objects.equals(oauthProvider, OAuthProvider.NAVER)) {
			email = oAuth2User.getAttribute("email").toString();
			name = oAuth2User.getAttribute("name").toString();
		} else if (Objects.equals(oauthProvider, OAuthProvider.KAKAO)) {
			Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
			email = kakaoAccount.get("email").toString();
			Map<String, Object> properties = oAuth2User.getAttribute("properties");
			name = properties.get("nickname").toString();
		}
		return new CustomOAuth2User(oauthProviderId, oauthProvider, email, name);
	}
	
	   private void handleAccountLinking(HttpServletRequest req, HttpServletResponse res, Authentication authentication, int userId, String clientIp) throws IOException {
	        
	        // 2. 새로 인증된 소셜 계정 정보를 가져온다.
	        CustomOAuth2User newOAuthUser = extractAuthentication(authentication);

	        // 3. DB에 새로운 OAuth 연결 정보를 저장한다. (service.linkNewOAuthAccount(loggedInUser.getUserId(), newOAuthUser))
	        signService.signUpLinkOAuth(userId, newOAuthUser, clientIp);
	        
	        // 4. 성공 후, 다시 마이페이지의 로그인 정보 화면으로 리다이렉트
	        log.info("* 계정 연결 성공 User ID: {}, New Provider: {}", userId, newOAuthUser.getOauthProvider());
	        redirectStrategy.sendRedirect(req, res, "/my/signInfo");
	    }

}