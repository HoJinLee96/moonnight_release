package net.chamman.moonnight.domain.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminNameRequestDto(
		
	    @NotBlank(message = "validation.user.name.required")
	    @Pattern(regexp = "^[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9 ]+$", message = "validation.user.name.invalid")
	    @Size(min = 2, max = 20, message = "validation.user.name.length")
		String name,
		
		int version
		) {

}
