package net.chamman.moonnight.global.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import net.chamman.moonnight.global.validator.NameValidator;

@Documented
@Constraint(validatedBy = NameValidator.class) 
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidName {

	// 유효성 검사 실패 시 보여줄 기본 메시지
	String message() default "이름 형식이 올바르지 않습니다.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}