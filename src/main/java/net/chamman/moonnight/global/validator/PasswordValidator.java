package net.chamman.moonnight.global.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import net.chamman.moonnight.global.annotation.ValidPassword;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String>{
	
	private static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[\\W_])[A-Za-z\\d\\W_]{8,60}$";
	
	@Override
	public void initialize(ValidPassword constraintAnnotation) {
		// 초기화 로직 필요하면 여기에 (지금은 없음)
	}
	
	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null || value.isBlank()) return false;
		return value.matches(PASSWORD_REGEX);
	}
}
