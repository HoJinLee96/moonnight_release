package net.chamman.moonnight.domain.question.admin;

import static net.chamman.moonnight.global.exception.HttpStatusCode.DELETE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.READ_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.UPDATE_SUCCESS;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.domain.answer.admin.dto.AdminAnswerResponseDto;
import net.chamman.moonnight.domain.question.Question;
import net.chamman.moonnight.domain.question.admin.dto.AdminQuestionModifyRequestDto;
import net.chamman.moonnight.domain.question.admin.dto.AdminQuestionResponseDto;
import net.chamman.moonnight.domain.question.admin.dto.AdminQuestionStatusUpdateRequestDto;
import net.chamman.moonnight.domain.question.admin.dto.AdminQuestionVersionRequestDto;
import net.chamman.moonnight.domain.question.dto.QuestionSimpleResponseDto;
import net.chamman.moonnight.global.security.principal.CustomAdminDetails;
import net.chamman.moonnight.global.util.ApiResponseDto;
import net.chamman.moonnight.global.util.ApiResponseFactory;

@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/admin/question")
@RestController
public class AdminQuestionController {

	private final AdminQuestionService adminQuestionService;
	private final ApiResponseFactory apiResponseFactory;
	private final Obfuscator obfuscator;

	@Operation(summary = "[관리자] 질문 리스트 조회")
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping
	public ResponseEntity<ApiResponseDto<List<QuestionSimpleResponseDto>>> getQuestionsByPage(
			@AuthenticationPrincipal CustomAdminDetails customAdminDetails, Pageable pageable) {

		List<Question> list = adminQuestionService.getQuestionsByPage(pageable);

		List<QuestionSimpleResponseDto> body = list.stream().map(this::convertToSimpleDto).toList();

		return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS, body));
	}

	@Operation(summary = "[관리자] 질문 검색 리스트 조회")
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/search")
	public ResponseEntity<ApiResponseDto<List<QuestionSimpleResponseDto>>> searchQuestionsByTitle(
			@AuthenticationPrincipal CustomAdminDetails customAdminDetails, @RequestParam String title,
			Pageable pageable) {

		List<Question> list = adminQuestionService.getQuestionsByTitle(title, pageable);

		List<QuestionSimpleResponseDto> body = list.stream().map(this::convertToSimpleDto).toList();

		return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS, body));
	}

	@Operation(summary = "[관리자] 질문 조회")
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/{questionId}")
	public ResponseEntity<ApiResponseDto<AdminQuestionResponseDto>> verifyPasswordForView(
			@PathVariable int questionId) {

		Question question = adminQuestionService.getQuestion(questionId);

		return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS, convertToDto(question)));
	}

	@Operation(summary = "[관리자] 질문 수정")
	@PreAuthorize("hasRole('ADMIN')")
	@PatchMapping("/{questionId}")
	public ResponseEntity<ApiResponseDto<AdminQuestionResponseDto>> modifyQuestion(
			@AuthenticationPrincipal CustomAdminDetails customAdminDetails, @PathVariable int questionId,
			@Valid @RequestBody AdminQuestionModifyRequestDto dto) {

		Question question = adminQuestionService.modifyQuestion(customAdminDetails.getAdminId(), questionId, dto);

		return ResponseEntity.ok(apiResponseFactory.success(UPDATE_SUCCESS, convertToDto(question)));
	}

	@Operation(summary = "[관리자] 질문 상태 변경")
	@PreAuthorize("hasRole('ADMIN')")
	@PatchMapping("/{questionId}/status")
	public ResponseEntity<ApiResponseDto<AdminQuestionResponseDto>> updateQuestionStatus(
			@AuthenticationPrincipal CustomAdminDetails customAdminDetails, @PathVariable int questionId,
			@Valid @RequestBody AdminQuestionStatusUpdateRequestDto dto) {

		Question question = adminQuestionService.updateQuestionStatus(customAdminDetails.getAdminId(), questionId, dto);

		return ResponseEntity.ok(apiResponseFactory.success(UPDATE_SUCCESS, convertToDto(question)));
	}

	@Operation(summary = "[관리자] 질문 삭제 (소프트 삭제)")
	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/{questionId}")
	public ResponseEntity<ApiResponseDto<Void>> deleteQuestion(
			@AuthenticationPrincipal CustomAdminDetails customAdminDetails, @PathVariable int questionId,
			@Valid @RequestBody AdminQuestionVersionRequestDto dto) {

		adminQuestionService.deleteQuestion(customAdminDetails.getAdminId(), questionId, dto);

		return ResponseEntity.ok(apiResponseFactory.success(DELETE_SUCCESS));
	}

	private AdminQuestionResponseDto convertToDto(Question question) {
		int encodedId = obfuscator.encode(question.getQuestionId());
		List<AdminAnswerResponseDto> answerDtos = question.getAnswers().stream().map(answer -> AdminAnswerResponseDto
				.from(answer, obfuscator.encode(answer.getAnswerId()), answer.getAdmin().getAdminId())).toList();
		return AdminQuestionResponseDto.from(question, encodedId, answerDtos);
	}

	private QuestionSimpleResponseDto convertToSimpleDto(Question question) {
		int encodedId = obfuscator.encode(question.getQuestionId());
		return QuestionSimpleResponseDto.from(question, encodedId);
	}
}
