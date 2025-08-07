package net.chamman.moonnight.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import net.chamman.moonnight.domain.admin.Admin;
import net.chamman.moonnight.domain.comment.Comment;
import net.chamman.moonnight.domain.comment.Comment.CommentStatus;
import net.chamman.moonnight.domain.estimate.Estimate;

public record CommentRequestDto(

		int estimateId,

		@NotBlank(message = "validation.comment.text.required") @Size(max = 250, message = "validation.comment.text.length") 
		String commentText

) {

	public Comment toEntity(Admin admin, Estimate estimate) {
		return Comment.builder()
				.admin(admin)
				.estimate(estimate)
				.commentText(commentText)
				.commentStatus(CommentStatus.ACTIVE)
				.build();
	}

}
