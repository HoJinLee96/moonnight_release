package net.chamman.moonnight.domain.answer.dto;

import java.time.LocalDateTime;

import net.chamman.moonnight.domain.answer.Answer;

public record AnswerResponseDto(
		int answerId, 
		String content, 
		LocalDateTime createdAt) {
	
	public static AnswerResponseDto from(Answer answer) {
		return new AnswerResponseDto(
				answer.getId(), 
				answer.getContent(), 
				answer.getCreatedAt()
				);
	}
}