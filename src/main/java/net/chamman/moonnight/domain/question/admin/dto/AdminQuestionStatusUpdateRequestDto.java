package net.chamman.moonnight.domain.question.admin.dto;

import net.chamman.moonnight.domain.question.Question.QuestionStatus;

public record AdminQuestionStatusUpdateRequestDto(
		
		QuestionStatus questionStatus,
		
		int version
		) {

}
