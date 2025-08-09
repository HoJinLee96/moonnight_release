package net.chamman.moonnight.domain.question.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminQuestionModifyRequestDto(
		
	    @NotBlank(message = "제목을 입력해주세요.")
	    @Size(max = 100, message = "제목은 100자를 넘을 수 없습니다.")
	    String title,

	    @NotBlank(message = "내용을 입력해주세요.")
	    @Size(max = 1000, message = "내용은 1000자를 넘을 수 없습니다.")
	    String content,
	    
	    int version
	    
		) {

}
