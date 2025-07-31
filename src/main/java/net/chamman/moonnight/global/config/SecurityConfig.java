package net.chamman.moonnight.global.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.security.fillter.JwtFilter;

@Configuration
@RequiredArgsConstructor
@Slf4j
@PropertySource("classpath:application.properties")
public class SecurityConfig {

	private final JwtFilter jwtFilter;
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
	public static final String[] PUBLIC_URIS = { "/api/*/public/**", "/", "/home", "/estimate", "/estimate/**",
			"/review", "/verify/**", "/sign/*" };

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

	// 관리자
	@Bean
	@Order(2)
	public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
		log.debug("* apiSecurityFilterChain() @Order(2) 필터 적용.");
		http.securityMatcher("/admin/**", "/api/admin/**").csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	// AccessDeniedHandler 빈 정의: 권한 없는 사용자 처리
	@Bean
	public AccessDeniedHandler accessDeniedHandler() {
//		return (request, response, accessDeniedException) -> {
//			response.sendRedirect(request.getContextPath() + "/home"); // 홈으로 보내버리기!
//		};
		return new CustomAccessDeniedHandler();
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
