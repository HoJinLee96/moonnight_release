package net.chamman.moonnight.domain.answer.admin;

import static net.chamman.moonnight.global.exception.HttpStatusCode.CREATE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.DELETE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.UPDATE_SUCCESS;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.domain.answer.Answer;
import net.chamman.moonnight.domain.answer.admin.dto.AdminAnswerCreateRequestDto;
import net.chamman.moonnight.domain.answer.admin.dto.AdminAnswerDeleteRequestDto;
import net.chamman.moonnight.domain.answer.admin.dto.AdminAnswerModifyRequestDto;
import net.chamman.moonnight.domain.answer.admin.dto.AdminAnswerResponseDto;
import net.chamman.moonnight.global.context.CustomRequestContextHolder;
import net.chamman.moonnight.global.security.principal.CustomAdminDetails;
import net.chamman.moonnight.global.util.ApiResponseDto;
import net.chamman.moonnight.global.util.ApiResponseFactory;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/answer")
public class AdminAnswerController {

	private final AdminAnswerService adminAnswerService;
	private final ApiResponseFactory apiResponseFactory;
	private final Obfuscator obfuscator;

	@Operation(summary = "[관리자] 답변 등록")
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/register")
	public ResponseEntity<ApiResponseDto<AdminAnswerResponseDto>> registerAnswer(
			@AuthenticationPrincipal CustomAdminDetails customAdminDetails,
			@Valid @RequestBody AdminAnswerCreateRequestDto dto) {

		String clientIp = CustomRequestContextHolder.getClientIp();

		Answer answer = adminAnswerService.registerAnswer(customAdminDetails.getAdminId(), dto, clientIp);

		return ResponseEntity
				.ok(apiResponseFactory.success(CREATE_SUCCESS, convertToDto(answer, customAdminDetails.getAdminId())));
	}

	@Operation(summary = "[관리자] 답변 수정")
	@PreAuthorize("hasRole('ADMIN')")
	@PatchMapping("/{answerId}")
	public ResponseEntity<ApiResponseDto<AdminAnswerResponseDto>> modifyAnswer(
			@AuthenticationPrincipal CustomAdminDetails customAdminDetails, @PathVariable int answerId,
			@Valid @RequestBody AdminAnswerModifyRequestDto dto) {

		Answer answer = adminAnswerService.modifyAnswer(customAdminDetails.getAdminId(), answerId, dto);

		return ResponseEntity
				.ok(apiResponseFactory.success(UPDATE_SUCCESS, convertToDto(answer, customAdminDetails.getAdminId())));
	}

	@Operation(summary = "[관리자] 답변 삭제 (하드 삭제)")
	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/{answerId}")
	public ResponseEntity<ApiResponseDto<Void>> deleteAnswer(
			@AuthenticationPrincipal CustomAdminDetails customAdminDetails, @PathVariable int answerId,
			@Valid @RequestBody AdminAnswerDeleteRequestDto dto) {

		adminAnswerService.deleteAnswer(customAdminDetails.getAdminId(), answerId, dto);
		
		return ResponseEntity.ok(apiResponseFactory.success(DELETE_SUCCESS));
	}

	private AdminAnswerResponseDto convertToDto(Answer answer, int adminId) {
		return AdminAnswerResponseDto.from(answer, obfuscator.encode(answer.getAnswerId()), adminId);
	}
}
