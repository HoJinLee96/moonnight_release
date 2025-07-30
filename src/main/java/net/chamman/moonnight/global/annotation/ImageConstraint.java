package net.chamman.moonnight.global.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import net.chamman.moonnight.global.validator.ImageValidator;

@Documented
@Constraint(validatedBy = ImageValidator.class)
@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ImageConstraint {
	String message() default "validation.image.invalid";
	Class<?>[] groups() default {};
	Class<? extends Payload>[] payload() default {};
}
