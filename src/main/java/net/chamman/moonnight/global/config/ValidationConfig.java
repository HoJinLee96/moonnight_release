package net.chamman.moonnight.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import jakarta.validation.Validator;

@Configuration
public class ValidationConfig {

    @Bean
    public static MethodValidationPostProcessor methodValidationPostProcessor(Validator validator) {
        MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        processor.setValidator(validator); 
        return processor;
    }

    @Bean
    public static Validator validator() {
        return new LocalValidatorFactoryBean(); // javax.validation 기반 검증기
    }
}