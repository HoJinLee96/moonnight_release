package net.chamman.moonnight.domain.comment.dto;

public record UpdateCommentRequestDto(
		String comment,
		int version
		) {

}
