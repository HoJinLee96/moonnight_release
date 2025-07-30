package net.chamman.moonnight.global.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.global.validator.ClientSpecificArgumentResolver;
import net.chamman.moonnight.global.validator.RedirectResolver;

@Configuration
@RequiredArgsConstructor
public class MvcConfig implements WebMvcConfigurer {
	
	private final ClientSpecificArgumentResolver clientSpecificArgumentResolver;
	private final RedirectResolver redirectResolver;
	
	
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
//		registry.addInterceptor(clientIpInterceptor)
//		.addPathPatterns("/**"); 
	}
	
	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(clientSpecificArgumentResolver);
		resolvers.add(redirectResolver);
	}
}
