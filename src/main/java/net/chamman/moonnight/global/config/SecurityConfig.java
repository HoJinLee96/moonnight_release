package net.chamman.moonnight.global.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.security.fillter.JwtAuthFilter;
import net.chamman.moonnight.global.security.fillter.JwtFilter;

@Configuration
@RequiredArgsConstructor
@Slf4j
@PropertySource("classpath:application.properties")
public class SecurityConfig {

	private final JwtFilter jwtFilter;
	private final JwtAuthFilter jwtAuthFilter;

	public static final String[] STATIC_RESOURCES = {"/css/**", "/js/**", "/images/**", "/favicon.ico", "/v3/api-docs/**", "/swagger-ui/**",
			"/swagger-ui.html", "/swagger-resources/**", "/webjars/**", "/openapi.yaml", "/.well-known/**"};

	// 관리자 로그인/회원가입 등 인증이 필요 없는 경로
	public static final String[] PUBLIC_ADMIN_URLS = { 
			"/admin/signIn", 
			"/admin/signUp1", 
			"/admin/signUp2", 
			"/admin/sign/stay", 
			"/admin/sign/stop", 
			"/admin/sign/delete", 
			"/admin/find/email",
			"/admin/find/password"
			// 기타 필요한 public 관리자 경로 추가
	};

	// 관리자 인증이 필요한 모든 경로 (PUBLIC_ADMIN_URLS 제외)
	public static final String[] ADMIN_URLS = { "/admin/**", "/api/admin/**" };

	// 사용자 인증이 필요한 특정 경로
	public static final String[] USER_PRIVATE_URLS = { "/api/*/private/**" };

	// 모든 접근 허용
    @Bean
    @Order(0)
    public SecurityFilterChain staticResourceFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(STATIC_RESOURCES) // 이 경로들에 대해서만 동작
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()) // 모든 요청 허용
            .csrf(AbstractHttpConfigurer::disable) // CSRF 보호 비활성화
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)); // 세션 사용 안함

        return http.build();
    }

	// 관리자 ROLE_ADMIN
    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(ADMIN_URLS) // 관리자 경로에 대해서만 동작
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_ADMIN_URLS).permitAll() // 로그인/회원가입 등은 허용
                .anyRequest().authenticated() // 그 외 모든 관리자 경로는 인증 필요
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class); // JwtFilter 적용

        return http.build();
    }

	// ROLE_AUTH 전용
    @Bean
    @Order(2)
    public SecurityFilterChain userPrivateFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(USER_PRIVATE_URLS) // 특정 사용자 API 경로에 대해서만 동작
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated()) // 모든 요청 인증 필요
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class); // JwtAuthFilter 적용

        return http.build();
    }
    
    @Bean
    @Order(3)
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
     * JwtFilter가 자동으로 서블릿 컨테이너에 등록되는 것을 방지합니다.
     * 이렇게 하면 우리가 SecurityFilterChain에 명시적으로 추가한 필터만 동작하게 됩니다.
     * @param filter JwtFilter 빈
     * @return FilterRegistrationBean
     */
    @Bean
    public FilterRegistrationBean<JwtFilter> jwtFilterRegistration(JwtFilter filter) {
        FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false); // 자동 등록 비활성화
        return registration;
    }

    /**
     * JwtAuthFilter가 자동으로 서블릿 컨테이너에 등록되는 것을 방지합니다.
     * @param filter JwtAuthFilter 빈
     * @return FilterRegistrationBean
     */
    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(JwtAuthFilter filter) {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false); // 자동 등록 비활성화
        return registration;
    }
}
