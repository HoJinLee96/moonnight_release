package net.chamman.moonnight.global.config;

import java.io.IOException;
import java.util.Map;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.security.fillter.JwtAuthFilter;
import net.chamman.moonnight.global.security.fillter.JwtFilter;
import net.chamman.moonnight.global.util.CookieUtil;

@Configuration
@RequiredArgsConstructor
@Slf4j
//@PropertySource("classpath:application.properties")
public class SecurityConfig {

	private final JwtFilter jwtFilter;
	private final JwtAuthFilter jwtAuthFilter;

	public static final String[] STATIC_RESOURCES = { "/css/**", "/js/**", "/images/**", "/favicon.ico",
			"/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/swagger-resources/**", "/webjars/**",
			"/openapi.yaml", "/.well-known/**" };

	// 관리자 로그인/회원가입 등 인증이 필요 없는 경로
	public static final String[] PUBLIC_ADMIN_URLS = { "/admin/signIn", "/admin/signUp1", "/admin/signUp2",
			"/admin/sign/stay", "/admin/sign/stop", "/admin/sign/delete", "/admin/find/email", "/admin/find/password",
			"/api/admin/public/signIn", "/api/admin/public/signUp/exist/email", "/api/admin/public/signUp/first",
			"/api/admin/public/signUp/exist/phone", "/api/admin/public/signUp/second", "/api/admin/public/find/email",
			"/api/admin/public/find/pw", "/api/admin/public/find/pw/by/phone", "/api/admin/public/find/pw/by/email",
			"/api/admin/public/find/pw/update" };

	// 관리자 인증이 필요한 모든 경로 (PUBLIC_ADMIN_URLS 제외)
//	public static final String[] ADMIN_URLS = { "/admin/**", "/api/admin/**" };

	// 사용자 인증이 필요한 특정 경로
	public static final String[] USER_PRIVATE_URLS = { "/api/*/private/**" };

	// 모든 접근 허용
	@Bean
	@Order(0)
	public SecurityFilterChain staticResourceFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher(STATIC_RESOURCES) // 이 경로들에 대해서만 동작
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll()) // 모든 요청 허용
				.csrf(AbstractHttpConfigurer::disable) // CSRF 보호 비활성화
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)); // 세션 사용 안함

		return http.build();
	}

	// 관리자 ROLE_ADMIN
	@Bean
	@Order(1)
	public SecurityFilterChain adminViewFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/admin/**") // 관리자 경로에 대해서만 동작
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(ex -> ex.authenticationEntryPoint(new CustomAdminAuthenticationEntryPoint()))
				.authorizeHttpRequests(auth -> auth.requestMatchers(PUBLIC_ADMIN_URLS).permitAll() // 로그인/회원가입 등은 허용
						.anyRequest().authenticated() // 그 외 모든 관리자 경로는 인증 필요
				).addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	// 관리자 ROLE_ADMIN
	@Bean
	@Order(2)
	public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/api/admin/**").csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(ex -> ex.authenticationEntryPoint(new CustomAdminAuthenticationEntryPoint()))
				.authorizeHttpRequests(auth -> auth.requestMatchers(PUBLIC_ADMIN_URLS).permitAll() // 로그인/회원가입 등은 허용
						.anyRequest().authenticated() // 그 외 모든 관리자 경로는 인증 필요
				).addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	// ROLE_AUTH 전용
	@Bean
	@Order(3)
	public SecurityFilterChain userPrivateFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher(USER_PRIVATE_URLS) // 특정 사용자 API 경로에 대해서만 동작
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth.anyRequest().authenticated()) // 모든 요청 인증 필요
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class); // JwtAuthFilter 적용

		return http.build();
	}

	@Bean
	@Order(4)
	public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
		http
				// securityMatcher를 설정하지 않으면 모든 요청이 이 필터 체인의 대상이 됨
				// (단, 위에서 먼저 처리된 요청들은 제외)
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll()); // 나머지 모든 요청은 허용

		return http.build();
	}

	/**
	 * JwtFilter가 자동으로 서블릿 컨테이너에 등록되는 것을 방지합니다. 이렇게 하면 SecurityFilterChain에 명시적으로 추가한 필터만 동작하게 됩니다.
	 */
	@Bean
	public FilterRegistrationBean<JwtFilter> jwtFilterRegistration(JwtFilter filter) {
		FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setEnabled(false); // 자동 등록 비활성화
		return registration;
	}

	/**
	 * JwtAuthFilter가 자동으로 서블릿 컨테이너에 등록되는 것을 방지합니다.
	 */
	@Bean
	public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(JwtAuthFilter filter) {
		FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setEnabled(false); // 자동 등록 비활성화
		return registration;
	}

	public class CustomAdminAuthenticationEntryPoint implements AuthenticationEntryPoint {

		private final ObjectMapper objectMapper = new ObjectMapper();

		@Override
		public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException authException)
				throws IOException, ServletException {

			String clientType = req.getHeader("X-Client-Type");
			boolean isMobileApp = clientType != null && clientType.contains("mobile");
			if (!isMobileApp) {
				CookieUtil.deleteCookie(req, res, "X-Access-Token");
				CookieUtil.deleteCookie(req, res, "X-Refresh-Token");
			}

			String requestURI = req.getRequestURI();

			if (requestURI.startsWith("/api/admin")) {
				res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				res.setContentType("application/json");
				res.setCharacterEncoding("UTF-8");

				Map<String, Object> body = Map.of("code", 4010, "message", "로그인이 필요합니다.");

				res.getWriter().write(objectMapper.writeValueAsString(body));
			} else {
				res.sendRedirect("/admin/signIn");
			}
		}
	}

}
