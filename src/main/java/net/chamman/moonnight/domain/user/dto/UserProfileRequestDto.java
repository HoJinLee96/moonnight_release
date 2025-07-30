package net.chamman.moonnight.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import net.chamman.moonnight.global.annotation.ValidBirth;

public record UserProfileRequestDto(
	    @NotBlank(message = "validation.user.name.required")
	    @Pattern(regexp = "^[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9 ]+$", message = "validation.user.name.invalid")
	    @Size(min = 2, max = 20, message = "validation.user.name.length")
		String name,
		
		@ValidBirth
		String birth,
		
		boolean marketingReceivedStatus
		) {

}
