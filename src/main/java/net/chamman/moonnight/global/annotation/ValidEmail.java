package net.chamman.moonnight.global.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import net.chamman.moonnight.global.validator.EmailValidator;

@Documented
@Constraint(validatedBy = EmailValidator.class) 
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmail {
	String message() default "validation.user.email.invalid";
	Class<?>[] groups() default {};
	Class<? extends Payload>[] payload() default {};
}