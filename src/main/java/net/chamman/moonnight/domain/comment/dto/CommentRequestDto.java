package net.chamman.moonnight.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.chamman.moonnight.domain.comment.Comment;
import net.chamman.moonnight.domain.comment.Comment.CommentStatus;
import net.chamman.moonnight.domain.estimate.Estimate;
import net.chamman.moonnight.domain.user.User;


public record CommentRequestDto(
    
    @NotNull
    int encodedEstimateId,
    
    @NotBlank(message = "validation.comment.text.required")
    @Size(max = 250, message = "validation.comment.text.length")
    String commentText
    
) {
    
    public Comment toEntity(User user, Estimate estiamte) {
      return Comment.builder()
              .user(user)
              .estimate(estiamte)
              .commentText(commentText)
              .commentStatus(CommentStatus.ACTIVE)
              .build();
    }

}
