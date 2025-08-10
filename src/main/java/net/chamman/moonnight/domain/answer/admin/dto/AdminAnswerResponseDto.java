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
	public static AdminAnswerResponseDto from(Answer answer, int encodedId, int adminId) {
        String authorName = answer.getAdmin().getName();
        boolean isMine = answer.verifyAdmin(adminId);
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
