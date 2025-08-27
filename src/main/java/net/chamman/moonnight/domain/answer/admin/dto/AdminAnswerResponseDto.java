package net.chamman.moonnight.domain.answer.admin.dto;

import java.time.LocalDateTime;

import net.chamman.moonnight.domain.answer.Answer;

public record AdminAnswerResponseDto(
		int answerId, 
		String authorName,
		boolean isMine,
		String content,
		int version,
		LocalDateTime createdAt
		) {
	public static AdminAnswerResponseDto from(Answer answer, int encodedId, int currentAdminId) {
        String authorName = answer.getAdmin().getName();
        boolean isMine = answer.verifyAdmin(currentAdminId);
		return new AdminAnswerResponseDto(
				encodedId,
				authorName,
				isMine,
				answer.getContent(),
				answer.getVersion(),
				answer.getCreatedAt()
				);
	}
}
