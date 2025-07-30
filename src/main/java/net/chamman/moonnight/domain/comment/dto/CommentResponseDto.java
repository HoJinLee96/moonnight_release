package net.chamman.moonnight.domain.comment.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.domain.comment.Comment;

@Builder
public record CommentResponseDto(
    
    int estimateId,
    int commentId, 
    String commentText,
    LocalDateTime createdAt, 
    LocalDateTime updatedAt,
    boolean isMine

) {
  public static CommentResponseDto fromEntity(Comment comment, int userId, Obfuscator obfuscator) {
    boolean isMine = comment.getUser().getUserId()==userId;
    return CommentResponseDto.builder()
    .estimateId(obfuscator.encode(comment.getEstimate().getEstimateId()))
    .commentId(obfuscator.encode(comment.getCommentId()))
    .commentText(comment.getCommentText())
    .createdAt(comment.getCreatedAt())
    .updatedAt(comment.getUpdatedAt())
    .isMine(isMine)
    .build();
  }
}
