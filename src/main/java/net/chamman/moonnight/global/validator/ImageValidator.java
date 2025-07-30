package net.chamman.moonnight.global.validator;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import net.chamman.moonnight.global.annotation.ImageConstraint;

public class ImageValidator implements ConstraintValidator<ImageConstraint, List<MultipartFile>> {
	
	@Override
	public boolean isValid(List<MultipartFile> images, ConstraintValidatorContext context) {
		if (images == null || images.isEmpty()) {
			return true;
		}
		
		if (images.size() > 10) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate("validation.image.length")
			.addConstraintViolation();
			return false;
		}
		
		for (MultipartFile file : images) {
			if (file.getSize() > 10 * 1024 * 1024) { // 10MB로 수정
				context.disableDefaultConstraintViolation();
				context.buildConstraintViolationWithTemplate("validation.image.size")
				.addConstraintViolation();
				return false;
			}
		}
		
		return true;
	}
}
