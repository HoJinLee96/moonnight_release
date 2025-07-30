package net.chamman.moonnight.global.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.oauth.HttpCookieOAuth2AuthorizationRequestRepository;
import net.chamman.moonnight.auth.oauth.OAuth2LoginSuccessHandler;
import net.chamman.moonnight.global.security.fillter.JwtAuthFilter;
import net.chamman.moonnight.global.security.fillter.JwtFilter;
import net.chamman.moonnight.global.security.fillter.SilentAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@Slf4j
@PropertySource("classpath:application.properties")
public class SecurityConfig {

	private final JwtFilter jwtFilter;
	private final JwtAuthFilter jwtAuthFilter;
	private final SilentAuthenticationFilter silentAuthenticationFilter;
	private final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;
	private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${naver-login.clientId}")
	private String naverClientId;
	@Value("${naver-login.clientSecret}")
	private String naverClientSecret;
	@Value("${naver-login.redirectUri}")
	private String naverRedirectUri;

	@Value("${kakao-api.restApiKey}")
	private String kakaoClientId;
	@Value("${kakao-api.clientSecret}")
	private String kakaoClientSecret;
	@Value("${kakao-login.redirectUri}")
	private String kakaoRedirectUri;

	// 누구나 접근 가능
	public static final String[] PUBLIC_URIS = { "/api/*/public/**", "/", "/home", "/estimate", "/estimate/**", "/review", "/verify/**",
			"/sign/*" };

	// 로그인 하면 안 되는 접근
	public static final String[] NON_SIGNIN_ONLY_URIS = { "/signin", "/signinBlank", "/signup*", "/signup/**",
			"/find/**", "/update/password/blank" };

	// 로그인 안 하면 안 되는 접근
	public static final String[] SIGNIN_ONLY_URIS = { "/my", "/my/**" };

	// 모든 접근 허용
	@Bean
	@Order(0)
	public SecurityFilterChain staticFilterChain(HttpSecurity http) throws Exception {
		log.debug("* staticFilterChain() @Order(0) 필터 적용.");

		http.securityMatcher("/css/**", "/js/**", "/images/**", "/favicon.ico", "/v3/api-docs/**", "/swagger-ui/**",
				"/swagger-ui.html", "/swagger-resources/**", "/webjars/**", "/openapi.yaml", "/.well-known/**")
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authz -> authz.anyRequest().permitAll());

		return http.build();
	}

	// ROLE_AUTH 전용
	@Bean
	@Order(1)
	public SecurityFilterChain authSecurityFilterChain(HttpSecurity http) throws Exception {
		log.debug("* authSecurityFilterChain() @Order(1) 필터 적용.");
		http.securityMatcher("/api/estimate/private/auth", "/api/spem/private/auth", "/api/spem/private/auth/**", "/api/estimate/private/auth/**")
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	// ROLE_LOCAL or ROLE_OAUTH
	@Bean
	@Order(2)
	public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
		log.debug("* apiSecurityFilterChain() @Order(2) 필터 적용.");
		http.securityMatcher("/api/*/private/**").csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	// OAUTH 로그인
	@Bean
	@Order(3)
	public SecurityFilterChain oauthSecurityFilterChain(HttpSecurity http) throws Exception {
		log.debug("* oauthSecurityFilterChain() @Order(3) 필터 적용.");

		return http.securityMatcher("/oauth2/**", "/login/oauth2/**").csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.oauth2Login(oauth2 -> oauth2
						.authorizationEndpoint(endpoint -> endpoint
								.authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository))
						.clientRegistrationRepository(clientRegistrationRepository())
						.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService()))
						.successHandler(oauth2LoginSuccessHandler))
				.build();
	}

	@Bean
	@Order(4)
	public SecurityFilterChain signFilterChain(HttpSecurity http) throws Exception {
		log.debug("* signFilterChain() @Order(4) 필터 적용.");

		http.securityMatcher(SIGNIN_ONLY_URIS).csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exceptions -> exceptions
		                .authenticationEntryPoint(authenticationEntryPoint()) // 1. 로그인 안 한 유저 처리
		                .accessDeniedHandler(accessDeniedHandler())       // 2. 권한 없는 유저 처리
		            )
				.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
				.addFilterBefore(silentAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	@Order(5)
	public SecurityFilterChain nonSignFilterChain(HttpSecurity http) throws Exception {
		log.debug("* nonSignFilterChain() @Order(5) 필터 적용.");

		http.securityMatcher(NON_SIGNIN_ONLY_URIS).csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exceptions -> exceptions.accessDeniedHandler(accessDeniedHandler()))
				.authorizeHttpRequests(authorize -> authorize.anyRequest().access((authz, context) -> {
					Authentication auth = authz.get();
					boolean isAnonymous = auth == null || !auth.isAuthenticated()
							|| (auth instanceof AnonymousAuthenticationToken);
					return new AuthorizationDecision(isAnonymous);
				})).addFilterBefore(silentAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	@Order(Integer.MAX_VALUE)
	public SecurityFilterChain fallbackFilterChain(HttpSecurity http) throws Exception {
		log.debug("* fallbackFilterChain() @Order(Integer.MAX_VALUE) 필터 적용. (catch-all)");

		http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
				.addFilterBefore(silentAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	// 사용자 정보를 커스터마이징 처리
	// OAuth2UserService는 OAuth 로그인 성공 후 AccessToken으로 유저 정보를 불러와 인증 객체를 만드는 커스터마이징
	// 포인트.
	// 위 코드는 네이버처럼 구조가 다른 JSON 응답에도 대응하기 위한 커스텀 로직이 포함된 형태.
	@SuppressWarnings("unchecked")
	@Bean
	public OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService() {
		log.debug("* customOAuth2UserService() 적용.");

		return userRequest -> {
			OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
			OAuth2User oAuth2User = delegate.loadUser(userRequest);
			String registrationId = userRequest.getClientRegistration().getRegistrationId();

			if (Objects.equals("naver", registrationId)) {
				Map<String, Object> attributes = oAuth2User.getAttributes();
				Map<String, Object> response = (Map<String, Object>) attributes.get("response");
				return new DefaultOAuth2User(Collections.singleton(new SimpleGrantedAuthority("ROLE_OAUTH")), response,
						"id");
			}
			return oAuth2User;
		};
	}

	@Bean
	public ClientRegistrationRepository clientRegistrationRepository() {
		return new InMemoryClientRegistrationRepository(this.naverClientRegistration(), this.kakaoClientRegistration());
	}

	private ClientRegistration naverClientRegistration() {
		return ClientRegistration.withRegistrationId("naver").clientId(naverClientId).clientSecret(naverClientSecret)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).redirectUri(naverRedirectUri)
				.scope("name", "email").authorizationUri("https://nid.naver.com/oauth2.0/authorize")
				.tokenUri("https://nid.naver.com/oauth2.0/token").userInfoUri("https://openapi.naver.com/v1/nid/me")
				.userNameAttributeName("response") // 식별값
				.clientName("Naver").build();
	}

	private ClientRegistration kakaoClientRegistration() {
		return ClientRegistration.withRegistrationId("kakao").clientId(kakaoClientId).clientSecret(kakaoClientSecret)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).redirectUri(kakaoRedirectUri)
				.scope("profile_nickname", "account_email").authorizationUri("https://kauth.kakao.com/oauth/authorize")
				.tokenUri("https://kauth.kakao.com/oauth/token").userInfoUri("https://kapi.kakao.com/v2/user/me")
				.userNameAttributeName("id").clientName("Kakao").build();
	}

	public class CustomAccessDeniedHandler implements AccessDeniedHandler {
		@Override
		public void handle(HttpServletRequest request, HttpServletResponse response,
				AccessDeniedException accessDeniedException) throws IOException {
			log.debug("* CustomAccessDeniedHandler 실행됨.");

			String clientType = request.getHeader("X-Client-Type");
			boolean isMobileApp = clientType != null && clientType.contains("mobile");

			if (isMobileApp) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				response.setContentType("application/json;charset=UTF-8");
				Map<String, Object> body = Map.of("statusCode", 403, "message", "접근 권한이 없습니다.");
				response.getWriter().write(objectMapper.writeValueAsString(body));
			} else {
				response.sendRedirect("/home");
			}
		}
	}

	// AccessDeniedHandler 빈 정의: 권한 없는 사용자 처리
	@Bean
	public AccessDeniedHandler accessDeniedHandler() {
//		return (request, response, accessDeniedException) -> {
//			response.sendRedirect(request.getContextPath() + "/home"); // 홈으로 보내버리기!
//		};
		return new CustomAccessDeniedHandler();
	}

	// AuthenticationEntryPoint 빈 정의: 인증 안된 사용자 리다이렉트
	@Bean
	public AuthenticationEntryPoint authenticationEntryPoint() {
		return new CustomAuthenticationEntryPoint();
	}

	public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
		@Override
		public void commence(HttpServletRequest request, HttpServletResponse response,
				AuthenticationException authException) throws IOException {
			log.debug("* AuthenticationEntryPoint 실행됨.");

			String clientType = request.getHeader("X-Client-Type");
			boolean isMobileApp = clientType != null && clientType.contains("mobile");

			if (isMobileApp) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
				response.setContentType("application/json;charset=UTF-8");
				Map<String, Object> body = Map.of("statusCode", 401, "message", "로그인이 필요합니다.");
				response.getWriter().write(objectMapper.writeValueAsString(body));
			} else {
				String uri = request.getRequestURI();
				String url = request.getRequestURL().toString();
				String queryString = request.getQueryString();
				String fullUrl = url + (queryString != null ? "?" + queryString : "");
				String encodedUrl = Base64.getEncoder().encodeToString(fullUrl.getBytes(StandardCharsets.UTF_8));

				if (Arrays.stream(SIGNIN_ONLY_URIS).anyMatch(uri::startsWith)) {
					response.sendRedirect("/signin?redirect=" + encodedUrl);
				} else {
					response.sendRedirect("/error/400");
				}
			}
		}
	}

}
