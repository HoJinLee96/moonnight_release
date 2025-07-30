package net.chamman.moonnight.auth.verification;

import static net.chamman.moonnight.global.exception.HttpStatusCode.SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.SUCCESS_NO_DATA;

import java.time.Duration;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.auth.verification.dto.VerificationEmailRequestDto;
import net.chamman.moonnight.auth.verification.dto.VerificationPhoneRequestDto;
import net.chamman.moonnight.global.annotation.ClientSpecific;
import net.chamman.moonnight.global.annotation.ValidEmail;
import net.chamman.moonnight.global.annotation.ValidPhone;
import net.chamman.moonnight.global.context.RequestContextHolder;
import net.chamman.moonnight.global.util.ApiResponseDto;
import net.chamman.moonnight.global.util.ApiResponseFactory;

@Tag(name = "VerificationController", description = "인증 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/verify")
public class VerificationController {
	
	private final VerificationService verificationService;
	private final ApiResponseFactory apiResponseFactory;
	
	@Operation(summary = "휴대폰 문자 인증번호 검사", description = "휴대폰 문자 인증번호 검사")
	@PostMapping("/public/compare/sms/uuid")
	public ResponseEntity<ApiResponseDto<Map<String,String>>> compareSmsVerification(
			@ClientSpecific("X-Verification-Id") String verificationId,
			@Valid @RequestBody VerificationPhoneRequestDto verificationPhoneRequestDto,
			HttpServletRequest request) {
		
		String clientIp = RequestContextHolder.getContext().getClientIp();
		boolean isMobileApp = RequestContextHolder.getContext().isMobileApp();
		
		String token = verificationService.compareSms(
				verificationId,
				verificationPhoneRequestDto.phone(),
				verificationPhoneRequestDto.verificationCode(),
				clientIp);
		
		if(isMobileApp) {
			return ResponseEntity.ok(apiResponseFactory.success(SUCCESS, Map.of("X-Verification-Phone-Token",token)));
		}else {
			ResponseCookie cookie = ResponseCookie.from("X-Verification-Phone-Token", token)
					.httpOnly(true)
					.secure(true)
					.path("/")
					.maxAge(Duration.ofMinutes(5))
					.sameSite("Lax")
					.build();
			
			return ResponseEntity
					.status(HttpStatus.OK) 
					.header(HttpHeaders.SET_COOKIE, cookie.toString())
					.body(apiResponseFactory.success(SUCCESS_NO_DATA, null));
		}
	}
	
	@Operation(summary = "이메일 인증 인증번호 검사", description = "이메일 인증 인증번호 검사")
	@PostMapping("/public/compare/email/uuid")
	public ResponseEntity<ApiResponseDto<Map<String,String>>> compareEmailVerification(
			@ClientSpecific("X-Verification-Id") String verificationId,
			@Valid @RequestBody VerificationEmailRequestDto verificationEmailRequestDto,
			HttpServletRequest request) {
		
		String clientIp = RequestContextHolder.getContext().getClientIp();
		boolean isMobileApp = RequestContextHolder.getContext().isMobileApp();
		
		String token = verificationService.compareEmail(
				verificationId,
				verificationEmailRequestDto.email(), 
				verificationEmailRequestDto.verificationCode(), 
				clientIp);
		
		if(isMobileApp) {
			return ResponseEntity.ok(apiResponseFactory.success(SUCCESS, Map.of("X-Verification-Email-Token",token)));
		}else {
			ResponseCookie cookie = ResponseCookie.from("X-Verification-Email-Token", token)
					.httpOnly(true)
					.secure(true)
					.path("/")
					.maxAge(Duration.ofMinutes(5))
					.sameSite("Lax")
					.build();
			
			return ResponseEntity
					.status(HttpStatus.OK) 
					.header(HttpHeaders.SET_COOKIE, cookie.toString())
					.body(apiResponseFactory.success(SUCCESS_NO_DATA, null));
		}
	}
	
	@Operation(summary = "휴대폰 문자 인증번호 발송", description = "휴대폰 문자 인증번호 발송")
	@PostMapping("/public/sms")
	public ResponseEntity<ApiResponseDto<Map<String,String>>> verifyToSms(
			@ValidPhone @RequestParam String phone,
			HttpServletRequest request) {
		
		String clientIp = RequestContextHolder.getContext().getClientIp();
		boolean isMobileApp = RequestContextHolder.getContext().isMobileApp();
		
		String encodingVerificationId = verificationService.sendSmsVerificationCode(phone, clientIp)+"";
		
		if(isMobileApp) {
			return ResponseEntity.ok(apiResponseFactory.success(SUCCESS, Map.of("X-Verification-Id",encodingVerificationId)));
		}else {
			ResponseCookie cookie = ResponseCookie.from("X-Verification-Id", encodingVerificationId)
					.httpOnly(true)
					.secure(true)
					.path("/")
					.maxAge(Duration.ofMinutes(3))
					.sameSite("Lax")
					.build();
			
			return ResponseEntity
					.status(HttpStatus.OK) 
					.header(HttpHeaders.SET_COOKIE, cookie.toString())
					.body(apiResponseFactory.success(SUCCESS_NO_DATA, null));
		}
	}
	
	@Operation(summary = "이메일 인증번호 발송", description = "이메일 인증번호 발송")
	@PostMapping("/public/email")
	public ResponseEntity<ApiResponseDto<Map<String,String>>> verifyToEmail(
			@ValidEmail @RequestParam String email,
			HttpServletRequest request) {
		
		String clientIp = RequestContextHolder.getContext().getClientIp();
		boolean isMobileApp = RequestContextHolder.getContext().isMobileApp();
		
		String encodingVerificationId = verificationService.sendEmailVerificationCode(email, clientIp)+"";
		
		if(isMobileApp) {
			return ResponseEntity.ok(apiResponseFactory.success(SUCCESS, Map.of("X-Verification-Id",encodingVerificationId)));
		}else {
			ResponseCookie cookie = ResponseCookie.from("X-Verification-Id", encodingVerificationId)
					.httpOnly(true)
					.secure(true)
					.path("/")
					.maxAge(Duration.ofMinutes(3))
					.sameSite("Lax")
					.build();
			
			return ResponseEntity
					.status(HttpStatus.OK) 
					.header(HttpHeaders.SET_COOKIE, cookie.toString())
					.body(apiResponseFactory.success(SUCCESS_NO_DATA, null));
		}
	}
	
}
