package net.chamman.moonnight.domain.answer.dto;

import java.time.LocalDateTime;

import net.chamman.moonnight.domain.answer.Answer;

public record AnswerResponseDto(
		int answerId, 
		String content,
		LocalDateTime createdAt) {
	
	public static AnswerResponseDto from(Answer answer, int encodedId) {
		return new AnswerResponseDto(
				encodedId,
				answer.getContent(), 
				answer.getCreatedAt()
				);
	}
}