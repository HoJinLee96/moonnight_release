package net.chamman.moonnight.domain.question.dto;

import java.time.LocalDateTime;
import java.util.List;

import net.chamman.moonnight.domain.answer.dto.AnswerResponseDto;
import net.chamman.moonnight.domain.question.Question;
import net.chamman.moonnight.domain.question.Question.QuestionStatus;

public record QuestionResponseDto(
    int questionId,
    String title,
    String content,
    QuestionStatus questionStatus,
    LocalDateTime createdAt,
    List<AnswerResponseDto> answers
) {
	  public static QuestionResponseDto from(Question question, int encodedId, List<AnswerResponseDto> answerDtos) {
	        return new QuestionResponseDto(
	            encodedId,
	            question.getTitle(),
	            question.getContent(),
	            question.getQuestionStatus(),
	            question.getCreatedAt(),
	            answerDtos
	        );
	    }
}