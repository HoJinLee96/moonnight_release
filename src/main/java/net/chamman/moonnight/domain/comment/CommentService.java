package net.chamman.moonnight.domain.comment;

import static net.chamman.moonnight.global.exception.HttpStatusCode.AUTHORIZATION_FAILED;
import static net.chamman.moonnight.global.exception.HttpStatusCode.COMMENT_NOT_FOUND;
import static net.chamman.moonnight.global.exception.HttpStatusCode.COMMENT_STATUS_DELETE;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.domain.admin.Admin;
import net.chamman.moonnight.domain.admin.AdminService;
import net.chamman.moonnight.domain.comment.Comment.CommentStatus;
import net.chamman.moonnight.domain.comment.dto.CommentRequestDto;
import net.chamman.moonnight.domain.comment.dto.CommentResponseDto;
import net.chamman.moonnight.domain.estimate.Estimate;
import net.chamman.moonnight.domain.estimate.EstimateService;
import net.chamman.moonnight.global.exception.ForbiddenException;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.global.exception.status.StatusDeleteException;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {
	
	private final CommentRepository commentRepository;
	private final AdminService adminService;
	private final EstimateService estimateService;
	private final Obfuscator obfuscator;
	
	/** 댓글 등록
	 * @param adminProvider
	 * @param email
	 * @param commentRequestDto
	 * 
	 * @throws NoSuchDataException {@link EstimateService#getEstimateById} 찾을 수 없는 견적서
	 * @throws StatusDeleteException {@link EstimateService#getEstimateById} 이미 삭제된 견적서
	 * 
	 * @return 댓글
	 */
	@Transactional
	public CommentResponseDto registerComment(int adminId, CommentRequestDto commentRequestDto) {
		
		Admin admin = adminService.getActiveAdminByAdminId(adminId);
		
		Estimate estimate = estimateService.getEstimateByIdNotDelete(commentRequestDto.encodedEstimateId());
		
		Comment comment = commentRequestDto.toEntity(admin, estimate);
		commentRepository.save(comment);
		
		return CommentResponseDto.fromEntity(comment, adminId, obfuscator);
	}
	
	/** 견적서의 댓글 리스트 조회
	 * @param encodedEstimateId
	 * @param adminId
	 * @return 댓글 리스트
	 */
	public List<CommentResponseDto> getCommentList(int encodedEstimateId, int adminId) {
		
		List<Comment> list = commentRepository.findByEstimate_EstimateId(obfuscator.decode(encodedEstimateId));
		
		return list.stream()
				.filter(e->e.getCommentStatus()!=CommentStatus.DELETE)
				.map(comment -> CommentResponseDto.fromEntity(comment, adminId, obfuscator))
				.collect(Collectors.toList());
	}
	
	/** 댓글 수정
	 * @param adminId
	 * @param encodedCommentId
	 * @param commentRequestDto
	 * 
	 * @throws NoSuchDataException {@link #getAuthorizedComment} 찾을 수 없는 데이터
	 * @throws StatusDeleteException {@link #getAuthorizedComment} 삭제된 댓글
	 * @throws ForbiddenException {@link #getAuthorizedComment} 댓글 권한 없음
	 */
	@Transactional
	public void updateComment(int encodedCommentId, CommentRequestDto commentRequestDto, int adminId) {
		
		Comment comment = getAuthorizedComment(adminId, encodedCommentId);
		comment.setCommentText(commentRequestDto.commentText());
	}
	
	/**
	 * @param adminId
	 * @param encodedCommentId
	 * 
	 * @throws NoSuchDataException {@link #getAuthorizedComment} 찾을 수 없는 데이터
	 * @throws StatusDeleteException {@link #getAuthorizedComment} 삭제된 댓글
	 * @throws ForbiddenException {@link #getAuthorizedComment} 댓글 권한 없음
	 */
	@Transactional
	public void deleteComment(int adminId, int encodedCommentId) {
		
		Comment comment = getAuthorizedComment(adminId, encodedCommentId);
		comment.setCommentStatus(CommentStatus.DELETE);
	}
	
	/** 댓글 Get
	 * @param adminId
	 * @param encodedCommentId
	 * @throws NoSuchDataException {@link #getAuthorizedComment} 찾을 수 없는 데이터
	 * @throws StatusDeleteException {@link #getAuthorizedComment} 삭제된 댓글
	 * @throws ForbiddenException {@link #getAuthorizedComment} 댓글 권한 없음
	 * @return 댓글
	 */
	private Comment getAuthorizedComment(int adminId, int encodedCommentId) {
		
		int commentId = obfuscator.decode(encodedCommentId);
		
		Comment comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new NoSuchDataException(COMMENT_NOT_FOUND,"일치하는 데이터 없음. encodedCommentId: " + encodedCommentId));
		
		if (comment.getCommentStatus() == Comment.CommentStatus.DELETE) {
			throw new StatusDeleteException(COMMENT_STATUS_DELETE,"이미 삭제된 댓글.");
		}
		
		if (comment.getAdmin().getAdminId() != adminId) {
			throw new ForbiddenException(AUTHORIZATION_FAILED,"댓글 조회 권한 이상. address.getAdmin().getAdminId(): "+comment.getAdmin().getAdminId()+"!= adminId: "+adminId);
		}
		return comment;
	}

}
