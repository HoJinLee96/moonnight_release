package net.chamman.moonnight.auth.adminSign;

import static net.chamman.moonnight.global.exception.HttpStatusCode.CREATE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.DELETE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.EMAIL_NOT_EXISTS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.PHONE_NOT_EXISTS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.SUCCESS_NO_DATA;
import static net.chamman.moonnight.global.exception.HttpStatusCode.UPDATE_SUCCESS;

import java.time.Duration;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.adminSign.dto.AdminSignInRequestDto;
import net.chamman.moonnight.auth.adminSign.dto.AdminSignUpRequestDto;
import net.chamman.moonnight.domain.admin.Admin;
import net.chamman.moonnight.domain.admin.dto.AdminResponseDto;
import net.chamman.moonnight.domain.admin.dto.FindAdminResponseDto;
import net.chamman.moonnight.global.annotation.ClientSpecific;
import net.chamman.moonnight.global.annotation.ValidEmail;
import net.chamman.moonnight.global.annotation.ValidPassword;
import net.chamman.moonnight.global.annotation.ValidPhone;
import net.chamman.moonnight.global.context.CustomRequestContextHolder;
import net.chamman.moonnight.global.security.principal.CustomAdminDetails;
import net.chamman.moonnight.global.util.ApiResponseDto;
import net.chamman.moonnight.global.util.ApiResponseFactory;
import net.chamman.moonnight.global.util.CookieUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil.MaskLevel;

@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class AdminSignController {

	private final AdminSignService adminSignService;
	private final ApiResponseFactory apiResponseFactory;

	@Operation(summary = "관리자 로그인")
	@PostMapping("/public/signIn")
	public ResponseEntity<ApiResponseDto<Map<String, String>>> amdinSignIn(
			@Valid @RequestBody AdminSignInRequestDto signInRequestDto, HttpServletRequest req,
			HttpServletResponse res) {

		String clientIp = CustomRequestContextHolder.getClientIp();
		boolean isMobileApp = CustomRequestContextHolder.isMobileApp();
		log.debug("* 관리자 로그인 요청. Email: [{}], Client IP: [{}], Admin-Agent: [{}]",
				LogMaskingUtil.maskEmail(signInRequestDto.email(), MaskLevel.MEDIUM), clientIp,
				isMobileApp ? "mobile" : "web");

		Map<String, String> signJwt = adminSignService.signInAndCreateJwt(signInRequestDto, clientIp);

		if (isMobileApp) {
			return ResponseEntity.ok(apiResponseFactory.success(SUCCESS, signJwt));
		} else {
			CookieUtil.addCookie(res, "X-Access-Token", signJwt.get("accessToken"), Duration.ofMinutes(120));
			CookieUtil.addCookie(res, "X-Refresh-Token", signJwt.get("refreshToken"), Duration.ofDays(14));

			return ResponseEntity.ok(apiResponseFactory.success(SUCCESS_NO_DATA, null));
		}
	}

	@Operation(summary = "관리자 로그아웃")
	@SecurityRequirement(name = "X-Access-Token")
	@SecurityRequirement(name = "X-Refresh-Token")
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/private/signOut")
	public ResponseEntity<ApiResponseDto<Void>> adminSignOut(
			@AuthenticationPrincipal CustomAdminDetails customAdminDetails,
			@ClientSpecific(value = "X-Access-Token") String accessToken,
			@ClientSpecific(value = "X-Refresh-Token") String refreshToken, HttpServletRequest req,
			HttpServletResponse res) {

		String clientIp = CustomRequestContextHolder.getClientIp();
		boolean isMobileApp = CustomRequestContextHolder.isMobileApp();
		log.debug("* 관리자 로그아웃 요청. Admin ID: [{}], AccessToken: [{}], Client IP: [{}], Admin-Agent: [{}]",
				customAdminDetails != null ? customAdminDetails.getAdminId() : "anonymous",
				LogMaskingUtil.maskToken(accessToken, MaskLevel.MEDIUM), clientIp, isMobileApp ? "mobile" : "web");

		adminSignService.signOut(customAdminDetails.getAdminId(), accessToken, refreshToken, clientIp);

		if (!isMobileApp) {
			CookieUtil.deleteCookie(req, res, "X-Access-Token");
			CookieUtil.deleteCookie(req, res, "X-Refresh-Token");
		}

		return ResponseEntity.ok(apiResponseFactory.success(SUCCESS_NO_DATA));
	}

	@Operation(summary = "이메일 중복 검사", description = "2070: 중복 없음, 4531: 이메일 중복.")
	@PostMapping("/public/signUp/exist/email")
	public ResponseEntity<ApiResponseDto<Void>> isEmailExistsForSignUp(@ValidEmail @RequestParam String email) {

		adminSignService.isEmailExistsForSignUp(email);
		
		return ResponseEntity.ok(apiResponseFactory.success(EMAIL_NOT_EXISTS));
	}

	@Operation(summary = "관리자 회원가입 1차", description = "이메일 인증, 비밀번호 입력")
	@SecurityRequirement(name = "X-Verification-Email-Token")
	@PostMapping("/public/signUp/first")
	public ResponseEntity<ApiResponseDto<Map<String, String>>> createSignUpToken(
			@ClientSpecific("X-Verification-Email-Token") String verificationEmailToken,
			@ValidEmail @RequestParam String email, @ValidPassword @RequestParam String password,
			@ValidPassword @RequestParam String confirmPassword, HttpServletRequest req, HttpServletResponse res) {

		String clientIp = CustomRequestContextHolder.getClientIp();
		boolean isMobileApp = CustomRequestContextHolder.isMobileApp();
		log.debug("* 관리자 회원가입 1차 요청. Email: [{}], VerificationEmailToken: [{}], Client IP: [{}], Admin-Agent: [{}]",
				LogMaskingUtil.maskEmail(email, MaskLevel.MEDIUM),
				LogMaskingUtil.maskToken(verificationEmailToken, MaskLevel.MEDIUM), clientIp,
				isMobileApp ? "mobile" : "web");

		String accessSignUpToken = adminSignService.createSignUpToken(email, password, confirmPassword, verificationEmailToken);

		if (isMobileApp) {
			return ResponseEntity
					.ok(apiResponseFactory.success(SUCCESS, Map.of("X-Access-SignUp-Token", accessSignUpToken)));
		} else {
			CookieUtil.deleteCookie(req, res, "X-Verification-Email-Token");
			CookieUtil.addCookie(res, "X-Access-SignUp-Token", accessSignUpToken, Duration.ofMinutes(20));

			return ResponseEntity.ok(apiResponseFactory.success(SUCCESS, null));
		}
	}

	@Operation(summary = "휴대폰 중복 검사", description = "2071: 중복 없음, 4532: 휴대폰 번호 중복.")
	@PostMapping("/public/signUp/exist/phone")
	public ResponseEntity<ApiResponseDto<Void>> isPhoneExistForSignUp(@ValidPhone @RequestParam String phone) {
		adminSignService.isPhoneExistForSignUp(phone);
		return ResponseEntity.ok(apiResponseFactory.success(PHONE_NOT_EXISTS));
	}

	@Operation(summary = "관리자 회원가입 2차", description = "휴대폰 문자 인증, 개인정보 입력")
	@SecurityRequirement(name = "X-Access-SignUp-Token")
	@SecurityRequirement(name = "X-Verification-Phone-Token")
	@PostMapping("/public/signUp/second")
	public ResponseEntity<ApiResponseDto<String>> signup(
			@ClientSpecific("X-Access-SignUp-Token") String accessSignUpToken,
			@ClientSpecific("X-Verification-Phone-Token") String verificationPhoneToken,
			@Valid @RequestBody AdminSignUpRequestDto signUpRequestDto, HttpServletRequest req,
			HttpServletResponse res) {

		String clientIp = CustomRequestContextHolder.getClientIp();
		boolean isMobileApp = CustomRequestContextHolder.isMobileApp();
		log.debug(
				"* 관리자 회원가입 2차 요청. AccessSignUpToken: [{}], VerificationPhoneToken: [{}], Client IP: [{}], Admin-Agent: [{}]",
				LogMaskingUtil.maskToken(accessSignUpToken, MaskLevel.MEDIUM),
				LogMaskingUtil.maskToken(verificationPhoneToken, MaskLevel.MEDIUM), clientIp,
				isMobileApp ? "mobile" : "web");

		String name = adminSignService.signUp(signUpRequestDto, accessSignUpToken, verificationPhoneToken, clientIp);

		if (!isMobileApp) {
			CookieUtil.deleteCookie(req, res, "X-Access-SignUp-Token");
			CookieUtil.deleteCookie(req, res, "X-Verification-Phone-Token");
		}

		return ResponseEntity.ok(apiResponseFactory.success(CREATE_SUCCESS, name));
	}

	@Operation(summary = "관리자 회원 탈퇴")
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/private/withdrawal")
	public ResponseEntity<ApiResponseDto<Void>> softDeleteAdmin(
			@AuthenticationPrincipal CustomAdminDetails customAdminDetails,
			@ClientSpecific(value = "X-Access-Token") String accessToken,
			@ClientSpecific(value = "X-Refresh-Token") String refreshToken,
			@ValidPassword @RequestParam String password, HttpServletRequest req, HttpServletResponse res) {

		String clientIp = CustomRequestContextHolder.getClientIp();
		boolean isMobileApp = CustomRequestContextHolder.isMobileApp();

		adminSignService.softDeleteAdmin(customAdminDetails.getAdminId(), password, accessToken, refreshToken,
				clientIp);

		if (!isMobileApp) {
			CookieUtil.deleteCookie(req, res, "X-Access-Token");
			CookieUtil.deleteCookie(req, res, "X-Refresh-Token");
		}
		return ResponseEntity.ok(apiResponseFactory.success(DELETE_SUCCESS));
	}

	@Operation(summary = "이메일 찾기", description = "휴대폰 인증 토큰 통해 이메일을 찾는다.")
	@SecurityRequirement(name = "VerificationPhoneToken")
	@Parameters(@Parameter(ref = "Phone"))
	@PostMapping("/public/find/email")
	public ResponseEntity<ApiResponseDto<FindAdminResponseDto>> findAdminEmail(
			@ClientSpecific("X-Verification-Phone-Token") String token,
			@ValidPhone @RequestParam String phone, HttpServletRequest req,
			HttpServletResponse res) {

		boolean isMobileApp = CustomRequestContextHolder.isMobileApp();

		Admin admin= adminSignService.getAdminByVerifiedPhone(phone, token);
		AdminResponseDto adminResponseDto = AdminResponseDto.from(admin);
		FindAdminResponseDto findAdminResponseDto = FindAdminResponseDto.from(adminResponseDto);

		if (!isMobileApp) {
			CookieUtil.deleteCookie(req, res, "X-Verification-Phone-Token");
		}
		return ResponseEntity.ok(apiResponseFactory.success(SUCCESS, findAdminResponseDto));
	}

	// 비밀번호 찾기 시 이메일 검사
	@Operation(summary = "비밀번호 찾기 1단계 이메일 검사")
	@PostMapping("/public/find/pw")
	public ResponseEntity<ApiResponseDto<Map<String, String>>> isEmailExistsForFindPassword(
			@ValidEmail @RequestParam String email) {

		String phone = adminSignService.getAdminPhoneByEmail(email);

		return ResponseEntity.ok(apiResponseFactory.success(SUCCESS, Map.of("phone", phone)));
	}

	@Operation(summary = "비밀번호 찾기 2단계", description = "휴대폰 인증 토큰 통해 비밀번호 변경할 자격이 있는지 검증 이 후 Access-FindPw-Token 토큰 발급.")
	@PostMapping("/public/find/pw/by/phone")
	public ResponseEntity<ApiResponseDto<Map<String, String>>> verifyPhoneAndCreateFindPwToken(
			@ClientSpecific("X-Verification-Phone-Token") String token, @ValidEmail @RequestParam String email,
			@ValidPhone @RequestParam String phone, HttpServletRequest req, HttpServletResponse res) {

		boolean isMobileApp = CustomRequestContextHolder.isMobileApp();

		String findPwToken = adminSignService.createFindPwTokenByVerifyPhone(email, phone, token);

		if (isMobileApp) {
			return ResponseEntity.ok(apiResponseFactory.success(SUCCESS, Map.of("X-Access-FindPw-Token", findPwToken)));
		} else {
			CookieUtil.deleteCookie(req, res, "X-Verification-Phone-Token");
			CookieUtil.addCookie(res, "X-Access-FindPw-Token", findPwToken, Duration.ofMinutes(10));

			return ResponseEntity.status(HttpStatus.OK).body(apiResponseFactory.success(SUCCESS_NO_DATA, null));
		}
	}

	@Operation(summary = "비밀번호 찾기 2단계", description = "이메일 인증 토큰 통해 비밀번호 변경할 자격이 있는지 검증 이 후 Access-FindPw-Token 토큰 발급.")
	@PostMapping("/public/find/pw/by/email")
	public ResponseEntity<ApiResponseDto<Map<String, String>>> verifyEmailAndCreateFindPwToken(
			@ClientSpecific("X-Verification-Email-Token") String token, @ValidEmail @RequestParam String email,
			HttpServletRequest req, HttpServletResponse res) {

		boolean isMobileApp = CustomRequestContextHolder.isMobileApp();

		String findPwToken = adminSignService.createFindPwTokenByVerifyEmail(email, token);

		if (isMobileApp) {
			return ResponseEntity.ok(apiResponseFactory.success(SUCCESS, Map.of("X-Access-FindPw-Token", findPwToken)));
		} else {
			CookieUtil.deleteCookie(req, res, "X-Verification-Email-Token");
			CookieUtil.addCookie(res, "X-Access-FindPw-Token", findPwToken, Duration.ofMinutes(10));

			return ResponseEntity.status(HttpStatus.OK).body(apiResponseFactory.success(SUCCESS_NO_DATA, null));
		}
	}

	@Operation(summary = "비밀번호 찾기 3단계 비밀번호 업데이트", description = "비밀번호 변경 토큰과 새로운 비밀번호 비밀번호를 검증하여 업데이트.")
	@PatchMapping("/public/find/pw/update")
	public ResponseEntity<ApiResponseDto<Void>> updatePasswordByFindPwToken(
			@ClientSpecific("X-Access-FindPw-Token") String accessFindPwToken,
			@ValidPassword @RequestParam String password, @ValidPassword @RequestParam String confirmPassword,
			HttpServletRequest req, HttpServletResponse res) {

		boolean isMobileApp = CustomRequestContextHolder.isMobileApp();

		String clientIp = CustomRequestContextHolder.getClientIp();

		adminSignService.updatePasswordByFindPwToken(accessFindPwToken, password, confirmPassword, clientIp);

		if (!isMobileApp) {
			CookieUtil.deleteCookie(req, res, "X-Access-FindPw-Token");
		}

		return ResponseEntity.ok(apiResponseFactory.success(UPDATE_SUCCESS));
	}

}
