package net.chamman.moonnight.global.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import net.chamman.moonnight.global.annotation.ValidName;

public class NameValidator implements ConstraintValidator<ValidName, String> {

	// 기존 @Pattern과 @Size 조건을 합친 정규식
	// ^: 문자열 시작
	// [가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9 ] : 허용할 문자 집합
	// {2,20}: 2글자 이상 20글자 이하
	// $: 문자열 끝
	private static final String NAME_REGEX = "^[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9 ]{2,20}$";

	@Override
	public void initialize(ValidName constraintAnnotation) {
		// 초기화 로직 (현재는 필요 없음)
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null || value.isBlank()) {
			return false;
		}

		return value.matches(NAME_REGEX);
	}
}