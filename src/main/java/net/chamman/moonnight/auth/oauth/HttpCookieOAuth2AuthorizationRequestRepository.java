package net.chamman.moonnight.auth.oauth;

import java.time.Duration;

import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import com.nimbusds.oauth2.sdk.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.util.CookieUtil;

@Slf4j
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    private static final Duration COOKIE_EXPIRE = Duration.ofMinutes(3);

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        log.debug("* 쿠키로부터 OAuth2 인증 정보 로드 시도.");
        return CookieUtil.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(cookie -> {
                    OAuth2AuthorizationRequest authRequest = CookieUtil.deserialize(cookie, OAuth2AuthorizationRequest.class);
                    log.debug("* 쿠키로부터 OAuth2 인증 정보 로드 완료. OAuth2AuthorizationRequest: {}", authRequest);
                    return authRequest;
                })
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequestCookies(request, response);
            return;
        }

        log.debug("* OAuth2 인증 요청 정보(state 포함) 쿠키에 저장.");
        // OAuth2AuthorizationRequest 객체 전체를 쿠키에 저장
        CookieUtil.addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, CookieUtil.serialize(authorizationRequest), COOKIE_EXPIRE);

        // redirect 파라미터도 별도의 쿠키에 저장
        String redirectUri = request.getParameter("redirect");
        if (StringUtils.isNotBlank(redirectUri)) {
            log.debug("* OAuth2 인증 요청 redirect 파라미터를 쿠키에 저장. Redirect URL: [{}]", redirectUri);
            CookieUtil.addCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUri, COOKIE_EXPIRE);
        }
    }

    // 이 메소드는 성공/실패 핸들러에서 명시적으로 호출될 때 쿠키를 지우는 용도로 사용할 수 있음
    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        return this.loadAuthorizationRequest(request);
    }

    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        log.debug("*Removing authorization request cookies.");
        CookieUtil.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        CookieUtil.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
    }
}