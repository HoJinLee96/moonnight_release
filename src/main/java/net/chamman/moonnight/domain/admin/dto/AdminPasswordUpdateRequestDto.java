package net.chamman.moonnight.domain.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminPasswordUpdateRequestDto(
		
		@NotBlank(message = "validation.user.password.required")
		@Pattern( regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^\\w\\s])[^\\s]{8,60}$", message = "validation.user.password.invalid")
		@Size(min = 8, max = 30, message = "validation.user.password.length")
		String newPassword,
		
		@NotBlank(message = "validation.user.password.required")
		@Pattern( regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^\\w\\s])[^\\s]{8,60}$", message = "validation.user.password.invalid")
		@Size(min = 8, max = 30, message = "validation.user.password.length")
		String confirmNewPassword,
		
		int version
		) {

}
