package net.chamman.moonnight.global.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import net.chamman.moonnight.global.validator.BirthValidator;

@Documented
@Constraint(validatedBy = BirthValidator.class) 
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBirth {
	String message() default "validation.user.birth.invalid";
	Class<?>[] groups() default {};
	Class<? extends Payload>[] payload() default {};

}
