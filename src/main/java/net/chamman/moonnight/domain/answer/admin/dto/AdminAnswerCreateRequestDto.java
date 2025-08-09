package net.chamman.moonnight.domain.answer.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminAnswerCreateRequestDto(
		
		int questionId,
		
	    @NotBlank(message = "내용을 입력해주세요.")
	    @Size(max = 1000, message = "내용은 1000자를 넘을 수 없습니다.")
	    String content
	    
		) {

}
