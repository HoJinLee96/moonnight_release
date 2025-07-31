package net.chamman.moonnight.domain.admin;

import static net.chamman.moonnight.global.exception.HttpStatusCode.EMAIL_NOT_EXISTS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.ILLEGAL_INPUT_VALUE;
import static net.chamman.moonnight.global.exception.HttpStatusCode.PHONE_NOT_EXISTS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.SUCCESS_NO_DATA;
import static net.chamman.moonnight.global.exception.HttpStatusCode.UPDATE_SUCCESS;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.domain.admin.dto.AdminResponseDto;
import net.chamman.moonnight.domain.admin.dto.FindAdminResponseDto;
import net.chamman.moonnight.global.annotation.ClientSpecific;
import net.chamman.moonnight.global.annotation.ValidEmail;
import net.chamman.moonnight.global.annotation.ValidPassword;
import net.chamman.moonnight.global.annotation.ValidPhone;
import net.chamman.moonnight.global.context.RequestContextHolder;
import net.chamman.moonnight.global.exception.IllegalRequestException;
import net.chamman.moonnight.global.security.principal.CustomAdminDetails;
import net.chamman.moonnight.global.util.ApiResponseDto;
import net.chamman.moonnight.global.util.ApiResponseFactory;
import net.chamman.moonnight.global.util.CookieUtil;

@Tag(name = "AdminInfoController", description = "유저 정보 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminInfoController {

	private final AdminService adminService;
	private final ApiResponseFactory apiResponseFactory;

	@Operation(summary = "휴대폰 번호에 일치하는 이메일 찾기", description = "휴대폰 인증 토큰 통해 이메일을 찾는다.")
	@SecurityRequirement(name = "VerificationPhoneToken")
	@Parameters(@Parameter(ref = "Phone"))
	@ApiResponses({ @ApiResponse(responseCode = "200", ref = "SuccessEmailResponse"),
			@ApiResponse(responseCode = "404", ref = "BadRequestTokenMissing") })
	@PostMapping("/public/find/email/by/phone")
	public ResponseEntity<ApiResponseDto<FindAdminResponseDto>> findAdminEmail(
			@Parameter(hidden = true) @ClientSpecific("X-Verification-Phone-Token") String token,
			@Parameter(hidden = true) @ValidPhone @RequestParam String phone, HttpServletRequest req,
			HttpServletResponse res) {

		AdminResponseDto adminResponseDto = adminService.getAdminByVerifiedPhone(phone, token);
		FindAdminResponseDto findAdminResponseDto = FindAdminResponseDto.fromEntity(adminResponseDto);

		System.out.println(findAdminResponseDto.toString());
		CookieUtil.deleteCookie(req, res, "X-Verification-Phone-Token");

		return ResponseEntity.ok(apiResponseFactory.success(SUCCESS, findAdminResponseDto));
	}

	@Operation(summary = "비밀번호 찾기 2단계", description = "휴대폰 인증 토큰 통해 비밀번호 변경할 자격이 있는지 검증 이 후 Access-FindPw-Token 토큰 발급.")
	@PostMapping("/public/find/pw/by/phone")
	public ResponseEntity<ApiResponseDto<Map<String, String>>> verifyPhoneAndCreateFindPwToken(
			@ClientSpecific("X-Verification-Phone-Token") String token, @ValidEmail @RequestParam String email,
			@ValidPhone @RequestParam String phone, HttpServletResponse res) {

		boolean isMobileApp = RequestContextHolder.getContext().isMobileApp();

		String findPwToken = adminService.createFindPwTokenByVerifyPhone(email, phone, token);

		if (isMobileApp) {
			return ResponseEntity.ok(apiResponseFactory.success(SUCCESS, Map.of("X-Access-FindPw-Token", findPwToken)));
		} else {
			CookieUtil.addCookie(res, "X-Access-FindPw-Token", findPwToken, Duration.ofMinutes(10));

			return ResponseEntity.status(HttpStatus.OK).body(apiResponseFactory.success(SUCCESS_NO_DATA, null));
		}
	}

	@Operation(summary = "비밀번호 찾기 2단계", description = "이메일 인증 토큰 통해 비밀번호 변경할 자격이 있는지 검증 이 후 Access-FindPw-Token 토큰 발급.")
	@PostMapping("/public/find/pw/by/email")
	public ResponseEntity<ApiResponseDto<Map<String, String>>> verifyEmailAndCreateFindPwToken(
			@ClientSpecific("X-Verification-Email-Token") String token, @ValidEmail @RequestParam String email,
			HttpServletResponse res) {

		boolean isMobileApp = RequestContextHolder.getContext().isMobileApp();

		String findPwToken = adminService.createFindPwTokenByVerifyEmail(email, token);

		if (isMobileApp) {
			return ResponseEntity.ok(apiResponseFactory.success(SUCCESS, Map.of("X-Access-FindPw-Token", findPwToken)));
		} else {
			CookieUtil.addCookie(res, "X-Access-FindPw-Token", findPwToken, Duration.ofMinutes(10));

			return ResponseEntity.status(HttpStatus.OK).body(apiResponseFactory.success(SUCCESS_NO_DATA, null));
		}
	}

	@Operation(summary = "비밀번호 업데이트", description = "비밀번호 변경 토큰과 새로운 비밀번호 비밀번호를 검증하여 업데이트.")
	@PatchMapping("/public/update/pw")
	public ResponseEntity<ApiResponseDto<Void>> updatePasswordByFindPwToken(
			@ClientSpecific("X-Access-FindPw-Token") String accessFindPwToken,
			@ValidPassword @RequestParam String password, @ValidPassword @RequestParam String confirmPassword,
			HttpServletRequest request) {

		if (!Objects.equals(password, confirmPassword)) {
			throw new IllegalRequestException(ILLEGAL_INPUT_VALUE,
					"새로운 두 비밀번호가 일치하지 않음. password: " + password + ", confirmPassword: " + confirmPassword);
		}

		String clientIp = RequestContextHolder.getContext().getClientIp();

		adminService.updatePasswordByFindPwToken(accessFindPwToken, password, clientIp);

		return ResponseEntity.ok(apiResponseFactory.success(UPDATE_SUCCESS));
	}

	@Operation(summary = "휴대폰 업데이트")
	@PatchMapping("/private/update/phone")
	public ResponseEntity<ApiResponseDto<Void>> updatePhoneByToken(
			@AuthenticationPrincipal CustomAdminDetails customAdminDetails,
			@ClientSpecific("X-Verification-Phone-Token") String verificationPhoneToken,
			@ValidPhone @RequestParam String phone, HttpServletRequest request) {

		String clientIp = RequestContextHolder.getContext().getClientIp();

		adminService.updatePhoneByVerification(customAdminDetails.getAdminId(), phone, verificationPhoneToken, clientIp);

		return ResponseEntity.ok(apiResponseFactory.success(UPDATE_SUCCESS));
	}

	// 이메일 중복 검사
	@Operation(summary = "이메일 중복 검사", description = "2070: 중복 없음, 4531: 이메일 중복.")
	@PostMapping("/public/exist/email")
	public ResponseEntity<ApiResponseDto<Void>> isEmailExists(@ValidEmail @RequestParam String email) {

		adminService.isEmailExistsForRegistration(email);
		return ResponseEntity.ok(apiResponseFactory.success(EMAIL_NOT_EXISTS));
	}
	
	// 비밀번호 찾기 시 이메일 검사
	@Operation(summary = "비밀번호 찾기 시 이메일 검사")
	@PostMapping("/public/exist/email/find/password")
	public ResponseEntity<ApiResponseDto<Map<String, String>>> isEmailExistsForFindPassword(
			@ValidEmail @RequestParam String email) {

		String phone = adminService.isEmailExistsForFindPassword(email);

		return ResponseEntity.ok(apiResponseFactory.success(SUCCESS, Map.of("phone", phone)));
	}

	@Operation(summary = "휴대폰 중복 검사", description = "2071: 중복 없음, 4532: 휴대폰 번호 중복.")
	@PostMapping("/public/exist/phone")
	public ResponseEntity<ApiResponseDto<Void>> isPhoneExists(@ValidPhone @RequestParam String phone) {
		adminService.isPhoneExists(phone);
		return ResponseEntity.ok(apiResponseFactory.success(PHONE_NOT_EXISTS));
	}

}
