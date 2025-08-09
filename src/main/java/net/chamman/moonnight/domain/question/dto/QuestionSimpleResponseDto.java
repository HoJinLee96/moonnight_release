package net.chamman.moonnight.domain.question.dto;

import java.time.LocalDateTime;

import net.chamman.moonnight.domain.question.Question;
import net.chamman.moonnight.domain.question.Question.QuestionStatus;

public record QuestionSimpleResponseDto(
		int questionId,
	    String title,
	    QuestionStatus questionStatus,
	    LocalDateTime createdAt,
	    int answerCount // 답변 개수만 포함
	) {
	    public static QuestionSimpleResponseDto from(Question question, int encodedId) {
	        return new QuestionSimpleResponseDto(
        		encodedId,
	            question.getTitle(),
	            question.getQuestionStatus(),
	            question.getCreatedAt(),
	            question.getAnswers().size()
	        );
	    }
	}