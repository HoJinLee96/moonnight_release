package net.chamman.moonnight.domain.comment;

import static net.chamman.moonnight.global.exception.HttpStatusCode.CREATE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.DELETE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.READ_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.READ_SUCCESS_NO_DATA;
import static net.chamman.moonnight.global.exception.HttpStatusCode.UPDATE_SUCCESS;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.domain.comment.dto.CommentRequestDto;
import net.chamman.moonnight.domain.comment.dto.CommentResponseDto;
import net.chamman.moonnight.global.security.principal.CustomAdminDetails;
import net.chamman.moonnight.global.util.ApiResponseDto;
import net.chamman.moonnight.global.util.ApiResponseFactory;

@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class CommentController {
	
	private final CommentService commentService;
	private final ApiResponseFactory apiResponseFactory;
	
//  댓글 등록
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/private/register")
	public ResponseEntity<ApiResponseDto<CommentResponseDto>> registerComment(
			@AuthenticationPrincipal CustomAdminDetails customAdminDetails, 
			@Valid @RequestBody CommentRequestDto commentRequestDto) {
		
		CommentResponseDto commentResponseDto = commentService.registerComment(customAdminDetails.getAdminId(), commentRequestDto);
		
		return ResponseEntity.ok(apiResponseFactory.success(CREATE_SUCCESS, commentResponseDto));
	}
	
//  견적의 댓글 목록 조회 
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/private/estimate/{estimateId}")
	public ResponseEntity<ApiResponseDto<List<CommentResponseDto>>> getCommentList(
			@AuthenticationPrincipal CustomAdminDetails customAdminDetails, 
			@PathVariable("estimateId") int encodedEstimateId) {
		List<CommentResponseDto> list = commentService.getCommentList(encodedEstimateId, customAdminDetails.getAdminId());
		if(list==null) {
			return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS_NO_DATA, null));
		}
		return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS, list));
	}
	
//  댓글 수정
	@PreAuthorize("hasRole('OAUTH') or hasRole('LOCAL')")
	@PatchMapping("/private/{commentId}")
	public ResponseEntity<ApiResponseDto<Void>> updateComment(
			@AuthenticationPrincipal CustomAdminDetails customAdminDetails, 
			@PathVariable("commentId") int encodedCommentId, 
			@Valid @RequestBody CommentRequestDto commentRequestDto) {
		
		commentService.updateComment(encodedCommentId, commentRequestDto, customAdminDetails.getAdminId());
		return ResponseEntity.ok(apiResponseFactory.success(UPDATE_SUCCESS));
	}
	
//  댓글 삭제
	@PreAuthorize("hasRole('OAUTH') or hasRole('LOCAL')")
	@DeleteMapping("/private/{commentId}")
	public ResponseEntity<ApiResponseDto<Void>> deleteComment(
			@AuthenticationPrincipal CustomAdminDetails customAdminDetails, 
			@PathVariable("commentId") int encodedCommentId) {
		
		commentService.deleteComment(customAdminDetails.getAdminId(), encodedCommentId);
		return ResponseEntity.ok(apiResponseFactory.success(DELETE_SUCCESS));
	}
	
}
