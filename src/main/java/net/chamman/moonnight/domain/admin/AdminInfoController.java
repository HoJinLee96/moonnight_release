package net.chamman.moonnight.domain.admin;

import static net.chamman.moonnight.global.exception.HttpStatusCode.UPDATE_SUCCESS;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.domain.admin.dto.AdminNameRequestDto;
import net.chamman.moonnight.domain.admin.dto.AdminPhoneRequestDto;
import net.chamman.moonnight.domain.admin.dto.AdminPasswordUpdateRequestDto;
import net.chamman.moonnight.global.annotation.ClientSpecific;
import net.chamman.moonnight.global.security.principal.CustomAdminDetails;
import net.chamman.moonnight.global.util.ApiResponseDto;
import net.chamman.moonnight.global.util.ApiResponseFactory;
import net.chamman.moonnight.global.util.CookieUtil;

@Tag(name = "AdminInfoController", description = "관리자 정보 관련 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminInfoController {

	private final AdminService adminService;
	private final ApiResponseFactory apiResponseFactory;

	@Operation(summary = "비밀번호 업데이트", description = "비밀번호 변경 토큰과 새로운 비밀번호 비밀번호를 검증하여 업데이트.")
	@PreAuthorize("hasRole('ADMIN')")
	@PatchMapping("/private/update/pw/by/email")
	public ResponseEntity<ApiResponseDto<Void>> updatePasswordByVerificationEmailToken(
			@AuthenticationPrincipal CustomAdminDetails customAdminDetails,
			@ClientSpecific("X-Verification-Email-Token") String verificationEmailToken, 
			@Valid @RequestBody AdminPasswordUpdateRequestDto dto,
			HttpServletRequest req, HttpServletResponse res) {
		
		log.debug("* 비밀번호 업데이트. AdminPasswordUpdateRequestDto: [{}]", dto);
		log.debug("* 비밀번호 업데이트. X-Verification-Email-Token: [{}]", verificationEmailToken);

		adminService.updatePasswordByEmail(customAdminDetails.getAdminId(), dto, verificationEmailToken);
		
		CookieUtil.deleteCookie(req, res, "X-Verification-Email-Token");

		return ResponseEntity.ok(apiResponseFactory.success(UPDATE_SUCCESS));
	}
	
	@Operation(summary = "비밀번호 업데이트", description = "비밀번호 변경 토큰과 새로운 비밀번호 비밀번호를 검증하여 업데이트.")
	@PreAuthorize("hasRole('ADMIN')")
	@PatchMapping("/private/update/pw/by/phone")
	public ResponseEntity<ApiResponseDto<Void>> updatePasswordByVerificationPhoneToken(
			@AuthenticationPrincipal CustomAdminDetails customAdminDetails,
			@ClientSpecific("X-Verification-Phone-Token") String verificationPhoneToken, 
			@Valid @RequestBody AdminPasswordUpdateRequestDto dto,
			HttpServletRequest req, HttpServletResponse res) {
		
		log.debug("* 비밀번호 업데이트. AdminPasswordUpdateRequestDto: [{}]", dto);
		log.debug("* 비밀번호 업데이트. X-Verification-Phone-Token: [{}]", verificationPhoneToken);

		adminService.updatePasswordByPhone(customAdminDetails.getAdminId(), dto, verificationPhoneToken);
		
		CookieUtil.deleteCookie(req, res, "X-Verification-Phone-Token");

		return ResponseEntity.ok(apiResponseFactory.success(UPDATE_SUCCESS));
	}

	@Operation(summary = "휴대폰 업데이트")
	@PreAuthorize("hasRole('ADMIN')")
	@PatchMapping("/private/update/phone")
	public ResponseEntity<ApiResponseDto<Void>> updatePhoneByToken(
			@AuthenticationPrincipal CustomAdminDetails customAdminDetails,
			@ClientSpecific("X-Verification-Phone-Token") String verificationPhoneToken,
			@Valid @RequestBody AdminPhoneRequestDto dto, 
			HttpServletRequest req, HttpServletResponse res) {
		
		log.debug("* 휴대폰 업데이트. AdminPhoneRequestDto: [{}]", dto);

		adminService.updatePhoneByVerifiedPhone(customAdminDetails.getAdminId(), dto, verificationPhoneToken);

		CookieUtil.deleteCookie(req, res, "X-Verification-Phone-Token");

		return ResponseEntity.ok(apiResponseFactory.success(UPDATE_SUCCESS));
	}
	
	@Operation(summary = "이름 업데이트")
	@PreAuthorize("hasRole('ADMIN')")
	@PatchMapping("/private/update/name")
	public ResponseEntity<ApiResponseDto<Void>> updateName(
			@AuthenticationPrincipal CustomAdminDetails customAdminDetails,
			@Valid @RequestBody AdminNameRequestDto dto, HttpServletRequest request) {

		log.debug("* 이름 업데이트. AdminNameRequestDto: [{}]", dto);
		
		adminService.updateName(customAdminDetails.getAdminId(), dto);

		return ResponseEntity.ok(apiResponseFactory.success(UPDATE_SUCCESS));
	}


}
