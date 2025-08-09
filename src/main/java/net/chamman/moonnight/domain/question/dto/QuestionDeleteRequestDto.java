package net.chamman.moonnight.domain.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuestionDeleteRequestDto(
		
		@NotBlank(message = "비밀번호를 입력해주세요.")
		@Size(max = 4, message = "비밀번호는 4자를 넘을 수 없습니다.")
		String password,
		
		int version
		) {

}
