package net.chamman.moonnight.global.config;

import java.io.IOException;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

@Configuration
public class PrimaryConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
//비밀번호를 해시할 때 몇 번 반복해서 계산할지를 정하는 값
		int strength = 12;
		return new BCryptPasswordEncoder(strength);
	}

	@Bean
	public MessageSource messageSource() throws IOException {
		ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
		messageSource.setBasenames("classpath:/messages/messages");
		messageSource.setDefaultEncoding("UTF-8");
		messageSource.setFallbackToSystemLocale(false);
		return messageSource;
	}

	@Bean
	public MessageSourceAccessor messageSourceAccessor() throws IOException {
		return new MessageSourceAccessor(messageSource());
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}