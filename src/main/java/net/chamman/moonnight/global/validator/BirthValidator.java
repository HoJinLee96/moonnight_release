package net.chamman.moonnight.global.validator;

import java.time.DateTimeException;
import java.time.LocalDate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import net.chamman.moonnight.global.annotation.ValidBirth;

public class BirthValidator implements ConstraintValidator<ValidBirth, String> {

	@Override
	public void initialize(ValidBirth constraintAnnotation) {
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null || value.isBlank()) {
			setMessage(context, "validation.user.birth.required");
			return false;
		}
		if (!value.matches("^\\d{8}$")) {
			setMessage(context, "validation.user.birth.invalid");
			return false;
		}

		try {
			int year = Integer.parseInt(value.substring(0, 4));
			int month = Integer.parseInt(value.substring(4, 6));
			int day = Integer.parseInt(value.substring(6, 8));

			LocalDate birthDate = LocalDate.of(year, month, day);
			LocalDate now = LocalDate.now();

			if (year < 1900) {
				setMessage(context, "validation.user.birth.too_old");
				return false;
			}
			if (birthDate.isAfter(now)) {
				setMessage(context, "validation.user.birth.future_not_allowed");
				return false;
			}

		} catch (DateTimeException | NumberFormatException e) {
			setMessage(context, "validation.user.birth.invalid_date");
			return false;
		}

		return true;
	}

	private void setMessage(ConstraintValidatorContext context, String messageKey) {
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(messageKey).addConstraintViolation();
	}

}
