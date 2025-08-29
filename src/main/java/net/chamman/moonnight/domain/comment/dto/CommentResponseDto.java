package net.chamman.moonnight.domain.comment.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import net.chamman.moonnight.domain.comment.Comment;

@Builder
public record CommentResponseDto(
    
    int estimateId,
    int commentId, 
    String commentText,
    LocalDateTime createdAt, 
    LocalDateTime updatedAt,
    int version,
    boolean isMine,
    String authorName

) {
  public static CommentResponseDto fromEntity(Comment comment, int encodedEstimateId, int encodedCommentId, int currentAdminId) {
    return CommentResponseDto.builder()
    .estimateId(encodedEstimateId)
    .commentId(encodedCommentId)
    .commentText(comment.getCommentText())
    .createdAt(comment.getCreatedAt())
    .updatedAt(comment.getUpdatedAt())
    .version(comment.getVersion())
    .isMine(comment.verifyAdmin(currentAdminId))
    .authorName(comment.getAdmin().getName())
    .build();
  }
}
