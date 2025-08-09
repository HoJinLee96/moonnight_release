package net.chamman.moonnight.domain.question.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
	  public static QuestionResponseDto from(Question question, int encodedId) {
	        return new QuestionResponseDto(
	            encodedId, // 전달받은 값을 그대로 사용
	            question.getTitle(),
	            question.getContent(),
	            question.getQuestionStatus(),
	            question.getCreatedAt(),
	            question.getAnswers().stream()
	                    .map(AnswerResponseDto::from)
	                    .collect(Collectors.toList())
	        );
	    }
}