package net.chamman.moonnight.global.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import net.chamman.moonnight.global.annotation.ValidId;

public class IdValidator implements ConstraintValidator<ValidId, Integer> {
	
	private static final String ID_REGEX = "^\\{1,10}$";
	
	@Override
	public void initialize(ValidId constraintAnnotation) {
		// 초기화 로직 필요하면 여기에 (지금은 없음)
	}
	
	@Override
	public boolean isValid(Integer value, ConstraintValidatorContext context) {
		if (value == null ) return false;
		String validIdStr = value+"";
		return validIdStr.matches(ID_REGEX);
	}
}
