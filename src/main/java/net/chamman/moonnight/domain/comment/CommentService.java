package net.chamman.moonnight.domain.comment;

import static net.chamman.moonnight.global.exception.HttpStatusCode.AUTHORIZATION_FAILED;
import static net.chamman.moonnight.global.exception.HttpStatusCode.COMMENT_NOT_FOUND;
import static net.chamman.moonnight.global.exception.HttpStatusCode.COMMENT_STATUS_DELETE;
import static net.chamman.moonnight.global.exception.HttpStatusCode.ESTIMATE_NOT_FOUND;

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
import net.chamman.moonnight.domain.comment.dto.UpdateCommentRequestDto;
import net.chamman.moonnight.domain.estimate.Estimate;
import net.chamman.moonnight.domain.estimate.EstimateRepository;
import net.chamman.moonnight.domain.estimate.EstimateService;
import net.chamman.moonnight.global.annotation.ActiveAdminOnly;
import net.chamman.moonnight.global.exception.ForbiddenException;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.global.exception.status.StatusDeleteException;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {

	private final CommentRepository commentRepository;
	private final EstimateRepository estimateRepository;
	private final AdminService adminService;
	private final Obfuscator obfuscator;

	/**
	 * 댓글 등록
	 * 
	 * @param adminProvider
	 * @param email
	 * @param commentRequestDto
	 * 
	 * @throws NoSuchDataException   {@link EstimateService#getEstimateById} 찾을 수 없는 견적서
	 * @throws StatusDeleteException {@link EstimateService#getEstimateById} 이미 삭제된 견적서
	 * 
	 * @return 댓글
	 */
	@ActiveAdminOnly
	@Transactional
	public Comment registerComment(int adminId, int encodedEstimateId, String commentText) {

		Admin admin = adminService.getActiveAdminById(adminId);

		Estimate estimate = estimateRepository.findById(obfuscator.decode(encodedEstimateId))
				.orElseThrow(() -> new NoSuchDataException(ESTIMATE_NOT_FOUND,
						"일치하는 데이터 없음. encodedEstimateId: " + encodedEstimateId));

		Comment comment = Comment.builder().admin(admin).estimate(estimate).commentText(commentText)
				.commentStatus(CommentStatus.ACTIVE).build();
		commentRepository.save(comment);

		return comment;
	}

	/**
	 * 견적서의 댓글 리스트 조회
	 * 
	 * @param encodedEstimateId
	 * 
	 * @return 댓글 리스트
	 */
	public List<Comment> getCommentList(int encodedEstimateId) {

		List<Comment> list = commentRepository.findByEstimate_EstimateId(obfuscator.decode(encodedEstimateId));

		return list.stream().filter(e -> e.getCommentStatus() != CommentStatus.DELETE).collect(Collectors.toList());
	}

	/**
	 * 댓글 수정
	 * 
	 * @param adminId
	 * @param encodedCommentId
	 * @param commentRequestDto
	 * 
	 * @throws NoSuchDataException   {@link #getAuthorizedComment} 찾을 수 없는 데이터
	 * @throws StatusDeleteException {@link #getAuthorizedComment} 삭제된 댓글
	 * @throws ForbiddenException    {@link #getAuthorizedComment} 댓글 권한 없음
	 */
	@ActiveAdminOnly
	@Transactional
	public Comment updateComment(int adminId, int encodedCommentId, UpdateCommentRequestDto dto) {

		Comment comment = getAuthorizedComment(adminId, encodedCommentId);

		comment.verifyVersion(dto.version());

		comment.modify(dto.comment());

		return comment;
	}

	/**
	 * @param adminId
	 * @param encodedCommentId
	 * 
	 * @throws NoSuchDataException   {@link #getAuthorizedComment} 찾을 수 없는 데이터
	 * @throws StatusDeleteException {@link #getAuthorizedComment} 삭제된 댓글
	 * @throws ForbiddenException    {@link #getAuthorizedComment} 댓글 권한 없음
	 */
	@ActiveAdminOnly
	@Transactional
	public void deleteComment(int adminId, int encodedCommentId, int version) {

		Comment comment = getAuthorizedComment(adminId, encodedCommentId);

		comment.verifyVersion(version);

		comment.updateStatusDelete();
	}

	/**
	 * adminId가 작성한 댓글 및 ACTIVE 상태
	 * 
	 * @param adminId
	 * @param encodedCommentId
	 * 
	 * @throws NoSuchDataException   {@link #getAuthorizedComment} 찾을 수 없는 데이터
	 * @throws StatusDeleteException {@link #getAuthorizedComment} 삭제된 댓글
	 * @throws ForbiddenException    {@link #getAuthorizedComment} 댓글 권한 없음
	 * 
	 * @return 댓글
	 */
	private Comment getAuthorizedComment(int adminId, int encodedCommentId) {

		int commentId = obfuscator.decode(encodedCommentId);

		Comment comment = commentRepository.findById(commentId).orElseThrow(
				() -> new NoSuchDataException(COMMENT_NOT_FOUND, "일치하는 데이터 없음. encodedCommentId: " + encodedCommentId));

		if (comment.getCommentStatus() == Comment.CommentStatus.DELETE) {
			throw new StatusDeleteException(COMMENT_STATUS_DELETE, "이미 삭제된 댓글.");
		}

		if (comment.getAdmin().getAdminId() != adminId) {
			throw new ForbiddenException(AUTHORIZATION_FAILED, "댓글 조회 권한 이상. address.getAdmin().getAdminId(): "
					+ comment.getAdmin().getAdminId() + "!= adminId: " + adminId);
		}
		return comment;
	}

}
