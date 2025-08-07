package net.chamman.moonnight.auth.sign;

import static net.chamman.moonnight.global.exception.HttpStatusCode.SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.SUCCESS_NO_DATA;

import java.time.Duration;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.annotation.ClientSpecific;
import net.chamman.moonnight.global.context.CustomRequestContextHolder;
import net.chamman.moonnight.global.security.principal.AuthDetails;
import net.chamman.moonnight.global.util.ApiResponseDto;
import net.chamman.moonnight.global.util.ApiResponseFactory;
import net.chamman.moonnight.global.util.CookieUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil.MaskLevel;

@Tag(name = "SignController", description = "로그인, 회원가입, 로그아웃 API")
@RestController
@RequestMapping("/api/sign")
@Slf4j
@RequiredArgsConstructor
public class SignController {

	private final ApiResponseFactory apiResponseFactory;
	private final SignService signService;

	@Operation(summary = "AUTH 로그인", description = "휴대폰 인증 통해 Auth JWT 발급")
	@SecurityRequirement(name = "X-Verification-Phone-Token")
	@PostMapping("/public/in/auth/sms")
	public ResponseEntity<ApiResponseDto<Map<String, String>>> signInAuthBySms(
			@ClientSpecific("X-Verification-Phone-Token") String verificationPhoneToken, HttpServletRequest req,
			HttpServletResponse res) {

		String clientIp = CustomRequestContextHolder.getClientIp();
		boolean isMobileApp = CustomRequestContextHolder.isMobileApp();
		log.debug("* AUTH SMS 로그인 요청. VerificationToken: [{}], Client IP: [{}], User-Agent: [{}]",
				LogMaskingUtil.maskToken(verificationPhoneToken, MaskLevel.NONE), clientIp,
				isMobileApp ? "mobile" : "web");

		String authToken = signService.signInAuthBySms(verificationPhoneToken, clientIp);

		if (isMobileApp) {
			return ResponseEntity.status(HttpStatus.OK)
					.body(apiResponseFactory.success(SUCCESS, Map.of("X-Auth-Token", authToken)));
		} else {
			CookieUtil.deleteCookie(req, res, "X-Verification-Phone-Token");
			CookieUtil.addCookie(res, "X-Auth-Token", authToken, Duration.ofMinutes(30));

			return ResponseEntity.status(HttpStatus.OK).body(apiResponseFactory.success(SUCCESS_NO_DATA, null));
		}
	}

	@Operation(summary = "AUTH 로그인", description = "이메일 인증 통해 Auth JWT 발급")
	@SecurityRequirement(name = "X-Verification-Email-Token")
	@PostMapping("/public/in/auth/email")
	public ResponseEntity<ApiResponseDto<Map<String, String>>> signInAuthByEmail(
			@ClientSpecific("X-Verification-Email-Token") String verificationEmailToken, HttpServletRequest req,
			HttpServletResponse res) {

		String clientIp = CustomRequestContextHolder.getClientIp();
		boolean isMobileApp = CustomRequestContextHolder.isMobileApp();
		log.debug("* AUTH Email 로그인 요청. VerificationToken: [{}], Client IP: [{}], User-Agent: [{}]",
				LogMaskingUtil.maskToken(verificationEmailToken, MaskLevel.NONE), clientIp,
				isMobileApp ? "mobile" : "web");

		String authToken = signService.signInAuthByEmail(verificationEmailToken, clientIp);

		if (isMobileApp) {
			return ResponseEntity.status(HttpStatus.OK)
					.body(apiResponseFactory.success(SUCCESS, Map.of("X-Auth-Token", authToken)));
		} else {
			CookieUtil.deleteCookie(req, res, "X-Verification-Email-Token");
			CookieUtil.addCookie(res, "X-Auth-Token", authToken, Duration.ofMinutes(30));

			return ResponseEntity.status(HttpStatus.OK).body(apiResponseFactory.success(SUCCESS_NO_DATA, null));
		}
	}

	@Operation(summary = "Auth 로그아웃", description = "로그아웃")
	@SecurityRequirement(name = "X-Auth-Token")
	@PreAuthorize("hasRole('AUTH')")
	@PostMapping("/private/out/auth")
	public ResponseEntity<ApiResponseDto<Void>> signOutAuth(@AuthenticationPrincipal AuthDetails authDetails,
			@ClientSpecific(value = "X-Auth-Token") String authToken, HttpServletRequest req, HttpServletResponse res) {

		String clientIp = CustomRequestContextHolder.getClientIp();
		boolean isMobileApp = CustomRequestContextHolder.isMobileApp();
		log.debug("* AUTH 로그아웃 요청. AuthToken: [{}], Client IP: [{}], User-Agent: [{}]",
				LogMaskingUtil.maskToken(authToken, MaskLevel.NONE), clientIp, isMobileApp ? "mobile" : "web");

		signService.signOutAuth(authToken, clientIp);

		if (!isMobileApp) {
			CookieUtil.deleteCookie(req, res, "X-Auth-Token");
		}

		return ResponseEntity.ok(apiResponseFactory.success(SUCCESS));
	}

}
