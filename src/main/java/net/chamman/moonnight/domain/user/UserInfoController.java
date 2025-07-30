package net.chamman.moonnight.domain.user;

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
import org.springframework.web.bind.annotation.RequestBody;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.auth.token.JwtProvider;
import net.chamman.moonnight.auth.token.TokenProvider;
import net.chamman.moonnight.domain.user.User.UserProvider;
import net.chamman.moonnight.domain.user.dto.FindUserResponseDto;
import net.chamman.moonnight.domain.user.dto.UserProfileRequestDto;
import net.chamman.moonnight.domain.user.dto.UserResponseDto;
import net.chamman.moonnight.global.annotation.ClientSpecific;
import net.chamman.moonnight.global.annotation.ValidEmail;
import net.chamman.moonnight.global.annotation.ValidPassword;
import net.chamman.moonnight.global.annotation.ValidPhone;
import net.chamman.moonnight.global.context.RequestContextHolder;
import net.chamman.moonnight.global.exception.IllegalRequestException;
import net.chamman.moonnight.global.security.principal.CustomUserDetails;
import net.chamman.moonnight.global.util.ApiResponseDto;
import net.chamman.moonnight.global.util.ApiResponseFactory;
import net.chamman.moonnight.global.util.CookieUtil;

@Tag(name = "UserInfoController", description = "유저 정보 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserInfoController {

	private final UserService userService;
	private final TokenProvider tokenProvider;
	private final JwtProvider jwtProvider;
	private final ApiResponseFactory apiResponseFactory;

	@Operation(summary = "휴대폰 번호에 일치하는 유저 이메일 찾기", description = "휴대폰 인증 토큰 통해 이메일을 찾는다.")
	@SecurityRequirement(name = "VerificationPhoneToken")
	@Parameters(@Parameter(ref = "Phone"))
	@ApiResponses({ @ApiResponse(responseCode = "200", ref = "SuccessEmailResponse"),
			@ApiResponse(responseCode = "404", ref = "BadRequestTokenMissing") })
	@PostMapping("/public/find/email/by/phone")
	public ResponseEntity<ApiResponseDto<FindUserResponseDto>> findLocalUserEmail(
			@Parameter(hidden = true) @ClientSpecific("X-Verification-Phone-Token") String token,
			@Parameter(hidden = true) @ValidPhone @RequestParam String phone, HttpServletRequest req,
			HttpServletResponse res) {

		UserResponseDto userResponseDto = userService.getUserByVerifiedPhone(phone, token);
		FindUserResponseDto findUserResponseDto = FindUserResponseDto.fromEntity(userResponseDto);

		System.out.println(findUserResponseDto.toString());
		CookieUtil.deleteCookie(req, res, "X-Verification-Phone-Token");

		return ResponseEntity.ok(apiResponseFactory.success(SUCCESS, findUserResponseDto));
	}

	@Operation(summary = "LOCAL 유저 비밀번호 찾기 2단계", description = "휴대폰 인증 토큰 통해 비밀번호 변경할 자격이 있는지 검증 이 후 Access-FindPw-Token 토큰 발급.")
	@PostMapping("/public/find/pw/by/phone")
	public ResponseEntity<ApiResponseDto<Map<String, String>>> verifyPhoneAndCreateFindPwToken(
			@ClientSpecific("X-Verification-Phone-Token") String token, @ValidEmail @RequestParam String email,
			@ValidPhone @RequestParam String phone, HttpServletResponse res) {

		boolean isMobileApp = RequestContextHolder.getContext().isMobileApp();

		String findPwToken = userService.createFindPwTokenByVerifyPhone(UserProvider.LOCAL, email, phone, token);

		if (isMobileApp) {
			return ResponseEntity.ok(apiResponseFactory.success(SUCCESS, Map.of("X-Access-FindPw-Token", findPwToken)));
		} else {
			CookieUtil.addCookie(res, "X-Access-FindPw-Token", findPwToken, Duration.ofMinutes(10));

			return ResponseEntity.status(HttpStatus.OK).body(apiResponseFactory.success(SUCCESS_NO_DATA, null));
		}
	}

	@Operation(summary = "LOCAL 유저 비밀번호 찾기 2단계", description = "이메일 인증 토큰 통해 비밀번호 변경할 자격이 있는지 검증 이 후 Access-FindPw-Token 토큰 발급.")
	@PostMapping("/public/find/pw/by/email")
	public ResponseEntity<ApiResponseDto<Map<String, String>>> verifyEmailAndCreateFindPwToken(
			@ClientSpecific("X-Verification-Email-Token") String token, @ValidEmail @RequestParam String email,
			HttpServletResponse res) {

		boolean isMobileApp = RequestContextHolder.getContext().isMobileApp();

		String findPwToken = userService.createFindPwTokenByVerifyEmail(UserProvider.LOCAL, email, token);

		if (isMobileApp) {
			return ResponseEntity.ok(apiResponseFactory.success(SUCCESS, Map.of("X-Access-FindPw-Token", findPwToken)));
		} else {
			CookieUtil.addCookie(res, "X-Access-FindPw-Token", findPwToken, Duration.ofMinutes(10));

			return ResponseEntity.status(HttpStatus.OK).body(apiResponseFactory.success(SUCCESS_NO_DATA, null));
		}
	}

	@Operation(summary = "유저 비밀번호 업데이트", description = "비밀번호 변경 토큰과 새로운 비밀번호 비밀번호를 검증하여 업데이트.")
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

		userService.updatePasswordByFindPwToken(accessFindPwToken, password, clientIp);

		return ResponseEntity.ok(apiResponseFactory.success(UPDATE_SUCCESS));
	}

	@Operation(summary = "유저 휴대폰 업데이트")
	@PatchMapping("/private/update/phone")
	public ResponseEntity<ApiResponseDto<Void>> updatePhoneByToken(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@ClientSpecific("X-Verification-Phone-Token") String verificationPhoneToken,
			@ValidPhone @RequestParam String phone, HttpServletRequest request) {

		String clientIp = RequestContextHolder.getContext().getClientIp();

		userService.updatePhoneByVerification(userDetails.getUserId(), phone, verificationPhoneToken, clientIp);

		return ResponseEntity.ok(apiResponseFactory.success(UPDATE_SUCCESS));
	}

	@Operation(summary = "유저 프로필 업데이트")
	@PatchMapping("/private/update/profile")
	public ResponseEntity<ApiResponseDto<Void>> updateProfile(
			@ClientSpecific("X-Access-Token") String accessToken,
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@Valid @RequestBody UserProfileRequestDto userProfileRequestDto, HttpServletRequest request) {

		String clientIp = RequestContextHolder.getContext().getClientIp();

		userService.updateProfile(userDetails.getUserId(), userProfileRequestDto.name(), userProfileRequestDto.birth(),
				userProfileRequestDto.marketingReceivedStatus(), clientIp);

		if (!Objects.equals(userDetails.getName(), userProfileRequestDto.name())) {
			long ttl = jwtProvider.getAccessTokenRemainingTime(accessToken);
			tokenProvider.addTokenBlacklist(accessToken, ttl, "UPDATE");
		}

		return ResponseEntity.ok(apiResponseFactory.success(UPDATE_SUCCESS));
	}

	// 이메일 중복 검사
	@Operation(summary = "이메일 중복 검사", description = "2070: 중복 없음, 4531: 이메일 중복.")
	@PostMapping("/public/exist/email")
	public ResponseEntity<ApiResponseDto<Void>> isEmailExists(@ValidEmail @RequestParam String email) {

		userService.isEmailExistsForRegistration(email);
		return ResponseEntity.ok(apiResponseFactory.success(EMAIL_NOT_EXISTS));
	}

	// 유저 통합 변경시 이메일 중복 검사
	@Operation(summary = "통합 유저 변경시 이메일 중복 검사", description = "2070: 중복 없음, 4531: 이메일 중복.")
	@PostMapping("/private/exist/email/convert")
	public ResponseEntity<ApiResponseDto<Void>> isEmailExistsForConvertToLocal(
			@AuthenticationPrincipal CustomUserDetails userDetails, @ValidEmail @RequestParam String email) {

		if (Objects.equals(userDetails.getEmail(), email)) {
			return ResponseEntity.ok(apiResponseFactory.success(EMAIL_NOT_EXISTS));
		}
		userService.isEmailExistsForRegistration(email);
		return ResponseEntity.ok(apiResponseFactory.success(EMAIL_NOT_EXISTS));
	}

	// 비밀번호 찾기 시 이메일 검사
	@Operation(summary = "비밀번호 찾기 시 이메일 검사")
	@PostMapping("/public/exist/email/find/password")
	public ResponseEntity<ApiResponseDto<Map<String, String>>> isEmailExistsForFindPassword(
			@ValidEmail @RequestParam String email) {

		String phone = userService.isEmailExistsForFindPassword(email);

		return ResponseEntity.ok(apiResponseFactory.success(SUCCESS, Map.of("phone", phone)));
	}

	@Operation(summary = "휴대폰 중복 검사", description = "2071: 중복 없음, 4532: 휴대폰 번호 중복.")
	@PostMapping("/public/exist/phone")
	public ResponseEntity<ApiResponseDto<Void>> isPhoneExists(@ValidPhone @RequestParam String phone) {
		userService.isPhoneExists(phone);
		return ResponseEntity.ok(apiResponseFactory.success(PHONE_NOT_EXISTS));
	}

}
