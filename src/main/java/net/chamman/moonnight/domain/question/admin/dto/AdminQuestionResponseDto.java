package net.chamman.moonnight.domain.question.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

import net.chamman.moonnight.domain.answer.admin.dto.AdminAnswerResponseDto;
import net.chamman.moonnight.domain.question.Question;
import net.chamman.moonnight.domain.question.Question.QuestionStatus;

public record AdminQuestionResponseDto(
	    int questionId,
	    String title,
	    String content,
	    QuestionStatus questionStatus,
	    LocalDateTime createdAt,
	    LocalDateTime updatedAt,
	    int version,
	    List<AdminAnswerResponseDto> answers
	    ) {
	  public static AdminQuestionResponseDto from(Question question, int encodedId, List<AdminAnswerResponseDto> answerDtos) {
	        return new AdminQuestionResponseDto(
	            encodedId,
	            question.getTitle(),
	            question.getContent(),
	            question.getQuestionStatus(),
	            question.getCreatedAt(),
	            question.getUpdatedAt(),
	            question.getVersion(),
	            answerDtos
	        );
	    }
}
