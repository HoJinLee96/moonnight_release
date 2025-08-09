package net.chamman.moonnight.domain.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// record로 선언하여 생성자, getter, equals(), hashCode(), toString()이 자동 생성됩니다.
public record QuestionCreateRequestDto(
		
	@NotBlank(message = "비밀번호를 입력해주세요.")
	@Size(max = 4, message = "비밀번호는 4자를 넘을 수 없습니다.")
	String password,
		
    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 100, message = "제목은 100자를 넘을 수 없습니다.")
    String title,

    @NotBlank(message = "내용을 입력해주세요.")
    @Size(max = 1000, message = "내용은 1000자를 넘을 수 없습니다.")
    String content

) {
	// "핵심 도메인(엔티티)은 외부 계층(DTO)에 대해 아무것도 알아서는 안 된다."
}