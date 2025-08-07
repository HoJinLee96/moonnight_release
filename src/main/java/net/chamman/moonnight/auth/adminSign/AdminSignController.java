package net.chamman.moonnight.auth.adminSign;

import static net.chamman.moonnight.global.exception.HttpStatusCode.CREATE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.DELETE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.ILLEGAL_INPUT_VALUE;
import static net.chamman.moonnight.global.exception.HttpStatusCode.SUCCESS;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.adminSign.dto.AdminSignInRequestDto;
import net.chamman.moonnight.auth.adminSign.dto.AdminSignUpRequestDto;
import net.chamman.moonnight.domain.admin.AdminService;
import net.chamman.moonnight.global.annotation.ClientSpecific;
import net.chamman.moonnight.global.annotation.ValidEmail;
import net.chamman.moonnight.global.annotation.ValidPassword;
import net.chamman.moonnight.global.context.CustomRequestContextHolder;
import net.chamman.moonnight.global.exception.HttpStatusCode;
import net.chamman.moonnight.global.exception.IllegalRequestException;
import net.chamman.moonnight.global.security.principal.CustomAdminDetails;
import net.chamman.moonnight.global.util.ApiResponseDto;
import net.chamman.moonnight.global.util.ApiResponseFactory;
import net.chamman.moonnight.global.util.CookieUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil.MaskLevel;

@RestController
@RequestMapping("/api/admin/sign")
@Slf4j
@RequiredArgsConstructor
public class AdminSignController {

	private final AdminSignService adminSignService;
	private final AdminService adminService;
	private final ApiResponseFactory apiResponseFactory;

	@Operation(summary = "관리자 로그인")
	@PostMapping("/public/in")
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
			return ResponseEntity.status(HttpStatus.OK).body(apiResponseFactory.success(SUCCESS, signJwt));
		} else {
			CookieUtil.addCookie(res, "X-Access-Token", signJwt.get("accessToken"), Duration.ofMinutes(120));
			CookieUtil.addCookie(res, "X-Refresh-Token", signJwt.get("refreshToken"), Duration.ofDays(14));

			return ResponseEntity.status(HttpStatus.OK)
					.body(apiResponseFactory.success(HttpStatusCode.SUCCESS_NO_DATA, null));
		}
	}

	@Operation(summary = "관리자 로그아웃")
	@SecurityRequirement(name = "X-Access-Token")
	@SecurityRequirement(name = "X-Refresh-Token")
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/private/out")
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

		return ResponseEntity.ok(apiResponseFactory.success(SUCCESS));
	}

	@Operation(summary = "관리자 회원가입 1차", description = "이메일 인증, 비밀번호 입력")
	@SecurityRequirement(name = "X-Verification-Email-Token")
	@PostMapping("/public/up/first")
	public ResponseEntity<ApiResponseDto<Map<String, String>>> signup1(
			@ClientSpecific("X-Verification-Email-Token") String verificationEmailToken,
			@ValidEmail @RequestParam String email, @ValidPassword @RequestParam String password,
			@ValidPassword @RequestParam String confirmPassword, HttpServletRequest req, HttpServletResponse res) {

		String clientIp = CustomRequestContextHolder.getClientIp();
		boolean isMobileApp = CustomRequestContextHolder.isMobileApp();
		log.debug("* 관리자 회원가입 1차 요청. Email: [{}], VerificationEmailToken: [{}], Client IP: [{}], Admin-Agent: [{}]",
				LogMaskingUtil.maskEmail(email, MaskLevel.MEDIUM),
				LogMaskingUtil.maskToken(verificationEmailToken, MaskLevel.MEDIUM), clientIp,
				isMobileApp ? "mobile" : "web");

		if (!Objects.equals(password, confirmPassword)) {
			throw new IllegalRequestException(ILLEGAL_INPUT_VALUE,
					"두 비밀번호가 일치하지 않음. password: " + password + ", confirmPassword: " + confirmPassword);
		}

		String accessSignUpToken = adminSignService.signUpToken(email, password, verificationEmailToken);

		if (isMobileApp) {
			return ResponseEntity
					.ok(apiResponseFactory.success(SUCCESS, Map.of("X-Access-SignUp-Token", accessSignUpToken)));
		} else {
			CookieUtil.deleteCookie(req, res, "X-Verification-Email-Token");
			CookieUtil.addCookie(res, "X-Access-SignUp-Token", accessSignUpToken, Duration.ofMinutes(20));

			return ResponseEntity.ok(apiResponseFactory.success(SUCCESS, null));
		}
	}

	@Operation(summary = "관리자 회원가입 2차", description = "휴대폰 문자 인증, 개인정보 입력")
	@SecurityRequirement(name = "X-Access-SignUp-Token")
	@SecurityRequirement(name = "X-Verification-Phone-Token")
	@PostMapping("/public/up/second")
	public ResponseEntity<ApiResponseDto<String>> signup2(
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

		CookieUtil.deleteCookie(req, res, "X-Access-SignUp-Token");
		CookieUtil.deleteCookie(req, res, "X-Verification-Phone-Token");

		return ResponseEntity.ok(apiResponseFactory.success(CREATE_SUCCESS, name));
	}

	@Operation(summary = "관리자 회원 탈퇴")
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/private/delete")
	public ResponseEntity<ApiResponseDto<Void>> deleteAdmin(
			@AuthenticationPrincipal CustomAdminDetails customAdminDetails,
			@ClientSpecific(value = "X-Access-Token") String accessToken,
			@ClientSpecific(value = "X-Refresh-Token") String refreshToken,
			@ValidPassword @RequestParam String password, HttpServletRequest req, HttpServletResponse res) {

		String clientIp = CustomRequestContextHolder.getClientIp();
		boolean isMobileApp = CustomRequestContextHolder.isMobileApp();

		adminService.confirmPassword(customAdminDetails.getAdminId(), password, clientIp);
		adminSignService.deleteAdmin(customAdminDetails.getAdminId(), clientIp);
		adminSignService.signOut(customAdminDetails.getAdminId(), accessToken, refreshToken, clientIp);

		if (!isMobileApp) {
			CookieUtil.deleteCookie(req, res, "X-Access-Token");
			CookieUtil.deleteCookie(req, res, "X-Refresh-Token");
		}
		return ResponseEntity.ok(apiResponseFactory.success(DELETE_SUCCESS));
	}

}
