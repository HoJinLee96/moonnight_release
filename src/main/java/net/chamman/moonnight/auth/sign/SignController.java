package net.chamman.moonnight.auth.sign;

import static net.chamman.moonnight.global.exception.HttpStatusCode.CREATE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.DELETE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.ILLEGAL_INPUT_VALUE;
import static net.chamman.moonnight.global.exception.HttpStatusCode.SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.SUCCESS_NO_DATA;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.sign.dto.ConvertToLocalRequestDto;
import net.chamman.moonnight.auth.sign.dto.SignInRequestDto;
import net.chamman.moonnight.auth.sign.dto.SignUpRequestDto;
import net.chamman.moonnight.auth.sign.dto.UnlinkOAuthRequestDto;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.domain.user.UserService;
import net.chamman.moonnight.global.annotation.ClientSpecific;
import net.chamman.moonnight.global.annotation.Redirect;
import net.chamman.moonnight.global.annotation.ValidEmail;
import net.chamman.moonnight.global.annotation.ValidPassword;
import net.chamman.moonnight.global.context.RequestContextHolder;
import net.chamman.moonnight.global.exception.HttpStatusCode;
import net.chamman.moonnight.global.exception.IllegalRequestException;
import net.chamman.moonnight.global.security.principal.AuthDetails;
import net.chamman.moonnight.global.security.principal.CustomUserDetails;
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

	private final SignService signService;
	private final UserService userService;
	private final ApiResponseFactory apiResponseFactory;

	@Operation(summary = "LOCAL 유저 로그인", description = "LOCAL 유저 로그인")
	@PostMapping("/public/in/local")
	public ResponseEntity<ApiResponseDto<Map<String, String>>> signInLocal(
			@Valid @RequestBody SignInRequestDto signInRequestDto, @Redirect String redirect, HttpServletRequest req,
			HttpServletResponse res) {

		String clientIp = RequestContextHolder.getContext().getClientIp();
		boolean isMobileApp = RequestContextHolder.getContext().isMobileApp();
		log.debug("* LOCAL 로그인 요청. Email: [{}], Client IP: [{}], User-Agent: [{}]",
				LogMaskingUtil.maskEmail(signInRequestDto.email(), MaskLevel.MEDIUM), clientIp,
				isMobileApp ? "mobile" : "web");

		Map<String, String> signJwt = signService.signInLocalAndCreateJwt(signInRequestDto, clientIp);

		if (isMobileApp) {
			return ResponseEntity.status(HttpStatus.OK).body(apiResponseFactory.success(SUCCESS, signJwt));
		} else {
			CookieUtil.addCookie(res, "X-Access-Token", signJwt.get("accessToken"), Duration.ofMinutes(120));
			CookieUtil.addCookie(res, "X-Refresh-Token", signJwt.get("refreshToken"), Duration.ofDays(14));

			return ResponseEntity.status(HttpStatus.OK)
					.body(apiResponseFactory.success(SUCCESS, Map.of("redirect", redirect)));
		}
	}

	@Operation(summary = "AUTH 로그인", description = "휴대폰 인증 통해 Auth JWT 발급")
	@SecurityRequirement(name = "X-Verification-Phone-Token")
	@PostMapping("/public/in/auth/sms")
	public ResponseEntity<ApiResponseDto<Map<String, String>>> signInAuthBySms(
			@ClientSpecific("X-Verification-Phone-Token") String verificationPhoneToken, HttpServletRequest req,
			HttpServletResponse res) {

		String clientIp = RequestContextHolder.getContext().getClientIp();
		boolean isMobileApp = RequestContextHolder.getContext().isMobileApp();
		log.debug("* AUTH SMS 로그인 요청. VerificationToken: [{}], Client IP: [{}], User-Agent: [{}]",
				LogMaskingUtil.maskToken(verificationPhoneToken, MaskLevel.MEDIUM), clientIp,
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

		String clientIp = RequestContextHolder.getContext().getClientIp();
		boolean isMobileApp = RequestContextHolder.getContext().isMobileApp();
		log.debug("* AUTH Email 로그인 요청. VerificationToken: [{}], Client IP: [{}], User-Agent: [{}]",
				LogMaskingUtil.maskToken(verificationEmailToken, MaskLevel.MEDIUM), clientIp,
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

	@Operation(summary = "LOCAL, OAUTH 로그아웃", description = "LOCAL, OAUTH 로그아웃")
	@SecurityRequirement(name = "X-Access-Token")
	@SecurityRequirement(name = "X-Refresh-Token")
	@PreAuthorize("hasRole('LOCAL') or hasRole('OAUTH')")
	@PostMapping("/private/out/local")
	public ResponseEntity<ApiResponseDto<Void>> signOut(@AuthenticationPrincipal CustomUserDetails user,
			@ClientSpecific(value = "X-Access-Token") String accessToken,
			@ClientSpecific(value = "X-Refresh-Token") String refreshToken, HttpServletRequest req,
			HttpServletResponse res) {

		String clientIp = RequestContextHolder.getContext().getClientIp();
		boolean isMobileApp = RequestContextHolder.getContext().isMobileApp();
		log.debug("* LOCAL 로그아웃 요청. User ID: [{}], AccessToken: [{}], Client IP: [{}], User-Agent: [{}]",
				user != null ? user.getUserId() : "anonymous", LogMaskingUtil.maskToken(accessToken, MaskLevel.MEDIUM),
				clientIp, isMobileApp ? "mobile" : "web");

		signService.signOut(user.getUserProvider().name(), user.getUsername(), accessToken, refreshToken, clientIp);

		if (!isMobileApp) {
			CookieUtil.deleteCookie(req, res, "X-Access-Token");
			CookieUtil.deleteCookie(req, res, "X-Refresh-Token");
		}

		return ResponseEntity.ok(apiResponseFactory.success(SUCCESS));
	}

	@Operation(summary = "Auth 로그아웃", description = "로그아웃")
	@SecurityRequirement(name = "X-Auth-Token")
	@PreAuthorize("hasRole('Auth')")
	@PostMapping("/public/out/auth")
	public ResponseEntity<ApiResponseDto<Void>> signOutAuth(@AuthenticationPrincipal AuthDetails authDetails,
			@ClientSpecific(value = "X-Auth-Token") String authToken, HttpServletRequest req, HttpServletResponse res) {

		String clientIp = RequestContextHolder.getContext().getClientIp();
		boolean isMobileApp = RequestContextHolder.getContext().isMobileApp();
		log.debug("* AUTH 로그아웃 요청. AuthToken: [{}], Client IP: [{}], User-Agent: [{}]",
				LogMaskingUtil.maskToken(authToken, MaskLevel.MEDIUM), clientIp, isMobileApp ? "mobile" : "web");

		signService.signOutAuth(authToken, clientIp);

		if (!isMobileApp) {
			CookieUtil.deleteCookie(req, res, "X-Auth-Token");
		}

		return ResponseEntity.ok(apiResponseFactory.success(SUCCESS));
	}

	@Operation(summary = "LOCAL 회원가입 1차", description = "이메일 인증, 비밀번호 입력")
	@SecurityRequirement(name = "X-Verification-Email-Token")
	@PostMapping("/public/up/first")
	public ResponseEntity<ApiResponseDto<Map<String, String>>> signup1(
			@ClientSpecific("X-Verification-Email-Token") String verificationEmailToken,
			@ValidEmail @RequestParam String email, @ValidPassword @RequestParam String password,
			@ValidPassword @RequestParam String confirmPassword, HttpServletRequest req, HttpServletResponse res) {

		String clientIp = RequestContextHolder.getContext().getClientIp();
		boolean isMobileApp = RequestContextHolder.getContext().isMobileApp();
		log.debug("* LOCAL 회원가입 1차 요청. Email: [{}], VerificationEmailToken: [{}], Client IP: [{}], User-Agent: [{}]",
				LogMaskingUtil.maskEmail(email, MaskLevel.MEDIUM),
				LogMaskingUtil.maskToken(verificationEmailToken, MaskLevel.MEDIUM), clientIp,
				isMobileApp ? "mobile" : "web");

		if (!Objects.equals(password, confirmPassword)) {
			throw new IllegalRequestException(ILLEGAL_INPUT_VALUE,
					"두 비밀번호들가 일치하지 않음. password: " + password + ", confirmPassword: " + confirmPassword);
		}

		Optional<User> existUser = userService.isEmailDeleted(email);
		String accessSignUpToken = signService.createSignUpToken(email, password, verificationEmailToken);
		HttpStatusCode resultCode = existUser.isPresent() ? HttpStatusCode.DELETED_EMAIL_EXISTS : SUCCESS;

		if (isMobileApp) {
			return ResponseEntity
					.ok(apiResponseFactory.success(resultCode, Map.of("X-Access-SignUp-Token", accessSignUpToken)));
		} else {
			CookieUtil.deleteCookie(req, res, "X-Verification-Email-Token");
			CookieUtil.addCookie(res, "X-Access-SignUp-Token", accessSignUpToken, Duration.ofMinutes(20));

			return ResponseEntity.ok(apiResponseFactory.success(resultCode, null));
		}
	}

	@Operation(summary = "LOCAL 회원가입 2차", description = "휴대폰 문자 인증, 개인정보 입력")
	@SecurityRequirement(name = "X-Access-SignUp-Token")
	@SecurityRequirement(name = "X-Verification-Phone-Token")
	@PostMapping("/public/up/second")
	public ResponseEntity<ApiResponseDto<String>> signup2(
			@ClientSpecific("X-Access-SignUp-Token") String accessSignUpToken,
			@ClientSpecific("X-Verification-Phone-Token") String verificationPhoneToken,
			@Valid @RequestBody SignUpRequestDto signUpRequestDto, HttpServletRequest req, HttpServletResponse res) {

		String clientIp = RequestContextHolder.getContext().getClientIp();
		boolean isMobileApp = RequestContextHolder.getContext().isMobileApp();
		log.debug(
				"* LOCAL 회원가입 2차 요청. AccessSignUpToken: [{}], VerificationPhoneToken: [{}], Client IP: [{}], User-Agent: [{}]",
				LogMaskingUtil.maskToken(accessSignUpToken, MaskLevel.MEDIUM),
				LogMaskingUtil.maskToken(verificationPhoneToken, MaskLevel.MEDIUM), clientIp,
				isMobileApp ? "mobile" : "web");

		String name = signService.signUpLocalUser(signUpRequestDto, accessSignUpToken, verificationPhoneToken,
				clientIp);

		CookieUtil.deleteCookie(req, res, "X-Access-SignUp-Token");
		CookieUtil.deleteCookie(req, res, "X-Verification-Phone-Token");

		return ResponseEntity.ok(apiResponseFactory.success(CREATE_SUCCESS, name));
	}

	@Operation(summary = "계정 복구 재가입", description = "휴대폰 문자 인증, 개인정보 입력")
	@SecurityRequirement(name = "X-Access-SignUp-Token")
	@SecurityRequirement(name = "X-Verification-Phone-Token")
	@PostMapping("/public/up/restoration")
	public ResponseEntity<ApiResponseDto<String>> signUpRestoration(
			@ClientSpecific(required = false, value = "X-Access-SignUp-Token") String accessSignUpToken,
			@ClientSpecific(required = false, value = "X-Access-SignUp-OAuth-Token") String accessSignUpOAuthToken,
			@ClientSpecific("X-Verification-Phone-Token") String verificationPhoneToken,
			@Valid @RequestBody SignUpRequestDto signUpRequestDto, HttpServletRequest req, HttpServletResponse res) {

		if (accessSignUpToken == null && accessSignUpOAuthToken == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(apiResponseFactory.error(HttpStatusCode.ILLEGAL_REQUEST, null));
		}

		String clientIp = RequestContextHolder.getContext().getClientIp();
		boolean isMobileApp = RequestContextHolder.getContext().isMobileApp();
		log.debug(
				"* LOCAL 회원가입 계정 복구 요청. AccessSignUpToken: [{}], AccessSignUpOAuthToken: [{}], VerificationPhoneToken: [{}], Client IP: [{}], User-Agent: [{}]",
				LogMaskingUtil.maskToken(accessSignUpToken, MaskLevel.MEDIUM),
				LogMaskingUtil.maskToken(accessSignUpOAuthToken, MaskLevel.MEDIUM),
				LogMaskingUtil.maskToken(verificationPhoneToken, MaskLevel.MEDIUM), clientIp,
				isMobileApp ? "mobile" : "web");

		String name = null;
		if (accessSignUpToken != null) {
			name = signService.signUpRestorationLocalUser(signUpRequestDto, accessSignUpToken, verificationPhoneToken,
					clientIp);

			CookieUtil.deleteCookie(req, res, "X-Access-SignUp-Token");

		} else if (accessSignUpOAuthToken != null) {
			name = signService.signUpRestorationOAuthUser(signUpRequestDto, accessSignUpOAuthToken,
					verificationPhoneToken, clientIp);

			CookieUtil.deleteCookie(req, res, "X-Access-SignUp-OAuth-Token");
		}

		CookieUtil.deleteCookie(req, res, "X-Verification-Phone-Token");
		return ResponseEntity.ok(apiResponseFactory.success(CREATE_SUCCESS, name));
	}

	@Operation(summary = "OAUTH 상세정보입력 회원가입", description = "OAUTH 상세정보입력 회원가입")
	@SecurityRequirement(name = "X-Access-SignUp-OAuth-Token")
	@PostMapping("/public/up/oauth")
	public ResponseEntity<ApiResponseDto<String>> signUpOAuth(
			@ClientSpecific("X-Access-SignUp-OAuth-Token") String accessSignUpOAuthToken,
			@ClientSpecific("X-Verification-Phone-Token") String verificationPhoneToken,
			@Valid @RequestBody SignUpRequestDto signUpRequestDto, HttpServletRequest req, HttpServletResponse res) {

		String clientIp = RequestContextHolder.getContext().getClientIp();
		boolean isMobileApp = RequestContextHolder.getContext().isMobileApp();
		log.debug(
				"* OAuth 회원가입 2차 요청. AccessSignUpOAuthToken: [{}], VerificationPhoneToken: [{}], Client IP: [{}], User-Agent: [{}]",
				LogMaskingUtil.maskToken(accessSignUpOAuthToken, MaskLevel.MEDIUM),
				LogMaskingUtil.maskToken(verificationPhoneToken, MaskLevel.MEDIUM), 
				LogMaskingUtil.maskIp(clientIp, MaskLevel.MEDIUM),
				isMobileApp ? "mobile" : "web");

		String name = signService.signUpOAuthUser(signUpRequestDto, accessSignUpOAuthToken, verificationPhoneToken,
				clientIp);

		CookieUtil.deleteCookie(req, res, "X-Access-SignUp-OAuth-Token");
		CookieUtil.deleteCookie(req, res, "X-Verification-Phone-Token");

		return ResponseEntity.ok(apiResponseFactory.success(CREATE_SUCCESS, name));
	}

	@Operation(summary = "로그인과 소셜 연동 동시")
	@PostMapping("/public/up/link/oauth")
	public ResponseEntity<ApiResponseDto<Map<String, String>>> signUpLinkOAuthBySignIn(
			@ClientSpecific("X-Link-OAuth-Token") String linkOAuthToken,
			@Valid @RequestBody SignInRequestDto signInRequestDto, @Redirect String redirect, HttpServletRequest req,
			HttpServletResponse res) {

		String clientIp = RequestContextHolder.getContext().getClientIp();
		boolean isMobileApp = RequestContextHolder.getContext().isMobileApp();

		log.debug("* 로그인과 소셜 연동 동시 요청. LinkOAuthToken: [{}], Client IP: [{}], User-Agent: [{}]",
				LogMaskingUtil.maskToken(linkOAuthToken, MaskLevel.MEDIUM),
				LogMaskingUtil.maskIp(clientIp, MaskLevel.MEDIUM),
				isMobileApp ? "mobile" : "web");

		Map<String, String> signJwt = signService.signUpLinkOAuth(signInRequestDto, linkOAuthToken, clientIp);

		if (isMobileApp) {
			return ResponseEntity.status(HttpStatus.OK).body(apiResponseFactory.success(SUCCESS, signJwt));
		} else {
			CookieUtil.addCookie(res, "X-Access-Token", signJwt.get("accessToken"), Duration.ofMinutes(120));
			CookieUtil.addCookie(res, "X-Refresh-Token", signJwt.get("refreshToken"), Duration.ofDays(14));

			return ResponseEntity.status(HttpStatus.OK)
					.body(apiResponseFactory.success(SUCCESS, Map.of("redirect", redirect)));
		}
	}

	@Operation(summary = "통합 계정으로 변경")
	@PreAuthorize("hasRole('OAUTH')")
	@PostMapping("/private/convertToLocal")
	public ResponseEntity<ApiResponseDto<Void>> convertToLocal(@AuthenticationPrincipal CustomUserDetails userDetails,
			@ClientSpecific("X-Verification-Email-Token") String verificationEmailToken,
			@Valid @RequestBody ConvertToLocalRequestDto convertToLocalRequestDto, HttpServletRequest req) {
		
		String clientIp = RequestContextHolder.getContext().getClientIp();
		boolean isMobileApp = RequestContextHolder.getContext().isMobileApp();
		
		log.debug("* 통합 계정으로 변경 요청. VerificationEmailToken: [{}], Client IP: [{}], User-Agent: [{}]",
				LogMaskingUtil.maskToken(verificationEmailToken, MaskLevel.MEDIUM),
				LogMaskingUtil.maskIp(clientIp, MaskLevel.MEDIUM),
				isMobileApp ? "mobile" : "web");

		if (!Objects.equals(convertToLocalRequestDto.password(), convertToLocalRequestDto.confirmPassword())) {
			throw new IllegalRequestException(ILLEGAL_INPUT_VALUE);
		}

		signService.convertToLocal(userDetails.getUserId(), verificationEmailToken, convertToLocalRequestDto.email(),
				convertToLocalRequestDto.password(), clientIp);

		return ResponseEntity.ok(apiResponseFactory.success(SUCCESS_NO_DATA));
	}

	@Operation(summary = "LOCAL 회원 탈퇴")
	@PreAuthorize("hasRole('LOCAL')")
	@PostMapping("/private/delete")
	public ResponseEntity<ApiResponseDto<Void>> deleteUser(@AuthenticationPrincipal CustomUserDetails userDetails,
			@ClientSpecific(value = "X-Access-Token") String accessToken,
			@ClientSpecific(value = "X-Refresh-Token") String refreshToken,
			@ValidPassword @RequestParam String password, HttpServletRequest req, HttpServletResponse res) {

		String clientIp = RequestContextHolder.getContext().getClientIp();
		boolean isMobileApp = RequestContextHolder.getContext().isMobileApp();

		userService.confirmPassword(userDetails.getUserId(), password, clientIp);
		signService.deleteUser(userDetails.getUserId(), clientIp);
		signService.signOut(userDetails.getUserProvider().name(), userDetails.getUsername(), accessToken, refreshToken,
				clientIp);

		if (!isMobileApp) {
			CookieUtil.deleteCookie(req, res, "X-Access-Token");
			CookieUtil.deleteCookie(req, res, "X-Refresh-Token");
		}
		return ResponseEntity.ok(apiResponseFactory.success(DELETE_SUCCESS));
	}

	@Operation(summary = "OAUTH 연동 해제")
	@PreAuthorize("hasRole('LOCAL')")
	@PostMapping("/private/unlink")
	public ResponseEntity<ApiResponseDto<Void>> unlinkOAuth(@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestBody @Valid UnlinkOAuthRequestDto unlinkOAuthRequestDto, HttpServletRequest req) {

		String clientIp = RequestContextHolder.getContext().getClientIp();
		
		userService.confirmPassword(userDetails.getUserId(), unlinkOAuthRequestDto.password(), clientIp);
		signService.unlinkOAuth(userDetails.getUserId(), unlinkOAuthRequestDto, clientIp);

		return ResponseEntity.ok(apiResponseFactory.success(DELETE_SUCCESS));
	}

}
