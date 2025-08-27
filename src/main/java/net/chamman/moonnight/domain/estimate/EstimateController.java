package net.chamman.moonnight.domain.estimate;

import static net.chamman.moonnight.global.exception.HttpStatusCode.CREATE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.DELETE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.READ_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.READ_SUCCESS_NO_DATA;
import static net.chamman.moonnight.global.exception.HttpStatusCode.UPDATE_SUCCESS;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.domain.estimate.dto.EstimateRegisterRequestDto;
import net.chamman.moonnight.domain.estimate.dto.EstimateResponseDto;
import net.chamman.moonnight.domain.estimate.dto.EstimateUpdateRequestDto;
import net.chamman.moonnight.domain.estimate.dto.SimpleEstimateRequestDto;
import net.chamman.moonnight.global.annotation.ClientSpecific;
import net.chamman.moonnight.global.annotation.ImageConstraint;
import net.chamman.moonnight.global.context.CustomRequestContextHolder;
import net.chamman.moonnight.global.exception.HttpStatusCode;
import net.chamman.moonnight.global.exception.IllegalRequestException;
import net.chamman.moonnight.global.security.principal.AuthDetails;
import net.chamman.moonnight.global.util.ApiResponseDto;
import net.chamman.moonnight.global.util.ApiResponseFactory;
import net.chamman.moonnight.global.util.CookieUtil;

@RestController
@RequestMapping("/api/estimate")
@MultipartConfig
@RequiredArgsConstructor
@Slf4j
public class EstimateController {

	private final EstimateService estimateService;
	private final ApiResponseFactory apiResponseFactory;
	private final Obfuscator obfuscator;

	@PermitAll
	@Operation(summary = "견적서 등록", description = "견적서 등록")
	@PostMapping("/public/register")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> registerEstimate(
			@Valid @RequestPart EstimateRegisterRequestDto dto,
			@Valid @ImageConstraint @RequestPart(required = false) List<MultipartFile> images,
			HttpServletRequest request) throws IOException, URISyntaxException {

		log.debug("* 견적서 등록 estimateRequestDto: {}", dto.toString());
		if (images != null) {
			images.forEach(path -> log.debug("* 견적서 imagesPath: {}", path.getName()));
		}

		String clientIp = CustomRequestContextHolder.getClientIp();

		Estimate estimate = estimateService.registerEstimate(dto, images, clientIp);

		return ResponseEntity.status(HttpStatus.OK)
				.body(apiResponseFactory.success(CREATE_SUCCESS, EstimateResponseDto.fromEntity(estimate, obfuscator)));
	}

	@PermitAll
	@Operation(summary = "간편 견적서 등록", description = "간편 견적서 등록")
	@PostMapping("/public/register/simple")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> registerSimpleEstimate(
			@Valid @RequestBody SimpleEstimateRequestDto simpleEstimateRequestDto, HttpServletRequest request) {

		String clientIp = CustomRequestContextHolder.getClientIp();

		log.debug("* 간편 견적 신청. clientIp: [{}]", clientIp);

		String trap = simpleEstimateRequestDto.trap();
		if (trap != null && !trap.isEmpty()) {
			log.warn("* 간편 견적 봇 감지 확인. clientIp: [{}], trap: [{}]", clientIp, trap);
			throw new IllegalRequestException(HttpStatusCode.ILLEGAL_REQUEST);
		}

		Estimate estimate = estimateService.registerSimpleEstimate(simpleEstimateRequestDto, clientIp);

		return ResponseEntity.status(HttpStatus.OK)
				.body(apiResponseFactory.success(CREATE_SUCCESS, EstimateResponseDto.fromEntity(estimate, obfuscator)));
	}

//  AUTH 토큰 통해 견적서 전체 조회
	@PreAuthorize("hasRole('AUTH')")
	@GetMapping("/private/auth")
	public ResponseEntity<ApiResponseDto<Page<EstimateResponseDto>>> getAllEstimateByAuth(
			@AuthenticationPrincipal AuthDetails authDetails,
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
			HttpServletRequest req, HttpServletResponse res) {

		Page<Estimate> list = estimateService.getPageEstimateByAuthNotDelete(authDetails.getRecipient(), pageable);

		if (list == null || list.isEmpty()) {
			return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS_NO_DATA, null));
		}

		Page<EstimateResponseDto> body = list.map(e -> EstimateResponseDto.fromEntity(e, obfuscator));

		return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS, body));
	}

//  AUTH 토큰 통해 견적서 단건 조회
	@PreAuthorize("hasRole('AUTH')")
	@GetMapping("/private/auth/{estimateId}")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> getEstimateByAuth(
			@AuthenticationPrincipal AuthDetails authDetails, @PathVariable("estimateId") int encodedEstimateId) {

		Estimate estimate = estimateService.getEstimateByIdAndAuthRecipient(encodedEstimateId,
				authDetails.getRecipient());

		return ResponseEntity
				.ok(apiResponseFactory.success(READ_SUCCESS, EstimateResponseDto.fromEntity(estimate, obfuscator)));
	}

//  비회원 견적서 조회
	@PermitAll
	@PostMapping("/public/guest")
	public ResponseEntity<?> getEstimateByGuest(@RequestParam("estimateId") int encodedEstimateId,
			@RequestParam String recipient) {

		Estimate estimate = estimateService.getEstimateByIdAndAuthRecipient(encodedEstimateId, recipient);

		return ResponseEntity
				.ok(apiResponseFactory.success(READ_SUCCESS, EstimateResponseDto.fromEntity(estimate, obfuscator)));
	}

//  AUTH 토큰 통해 견적서 수정
	@PreAuthorize("hasRole('AUTH')")
	@PatchMapping("/private/auth/update/{estimateId}")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> updateEstimateByAuth(
			@AuthenticationPrincipal AuthDetails authDetails, @PathVariable("estimateId") int encodedEstimateId,
			@Valid @RequestPart EstimateUpdateRequestDto dto,
			@ImageConstraint @RequestPart(value = "images", required = false) List<MultipartFile> images)
			throws IOException {

		Estimate estimate = estimateService.updateEstimateByAuthRecipient(encodedEstimateId, dto, images,
				authDetails.getRecipient());

		log.debug("* Estimate: [{}]", estimate);

		return ResponseEntity
				.ok(apiResponseFactory.success(UPDATE_SUCCESS, EstimateResponseDto.fromEntity(estimate, obfuscator)));
	}

//  AUTH 토큰 통해 견적서 조회 이후 휴대폰 인증 해제
	@PreAuthorize("hasRole('AUTH')")
	@PatchMapping("/private/auth/clear/phone/{estimateId}")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> clearEstimatePhone(
			@AuthenticationPrincipal AuthDetails authDetails, @PathVariable("estimateId") int encodedEstimateId,
			@RequestParam int version) {

		Estimate estimate = estimateService.clearEstimatePhone(encodedEstimateId, version);

		log.debug("* Estimate: [{}]", estimate);

		return ResponseEntity
				.ok(apiResponseFactory.success(UPDATE_SUCCESS, EstimateResponseDto.fromEntity(estimate, obfuscator)));
	}

//  AUTH 토큰 통해 견적서 조회 이후 휴대폰 인증 토큰 통해 휴대폰 수정
	@PreAuthorize("hasRole('AUTH')")
	@PatchMapping("/private/auth/update/phone/{estimateId}")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> updateEstimatePhoneByVerification(
			@AuthenticationPrincipal AuthDetails authDetails, @PathVariable("estimateId") int encodedEstimateId,
			@ClientSpecific("X-Verification-Phone-Token") String verificationPhoneToken, @RequestParam int version,
			HttpServletRequest req, HttpServletResponse res) {

		Estimate estimate = estimateService.updateEstimatePhoneByVerificationPhone(encodedEstimateId,
				verificationPhoneToken, version);

		log.debug("* Estimate: [{}]", estimate);

		CookieUtil.deleteCookie(req, res, "X-Verification-Phone-Token");

		return ResponseEntity
				.ok(apiResponseFactory.success(UPDATE_SUCCESS, EstimateResponseDto.fromEntity(estimate, obfuscator)));
	}

//  AUTH 토큰 통해 견적서 조회 이후 이메일 인증 해제
	@PreAuthorize("hasRole('AUTH')")
	@PatchMapping("/private/auth/clear/email/{estimateId}")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> clearEstimateEmail(
			@AuthenticationPrincipal AuthDetails authDetails, @PathVariable("estimateId") int encodedEstimateId,
			@RequestParam int version) {

		Estimate estimate = estimateService.clearEstimateEmail(encodedEstimateId, version);

		log.debug("* Estimate: [{}]", estimate);

		return ResponseEntity
				.ok(apiResponseFactory.success(UPDATE_SUCCESS, EstimateResponseDto.fromEntity(estimate, obfuscator)));
	}

//  AUTH 토큰 통해 견적서 조회 이후 이메일 인증 토큰 통해 이메일 수정
	@PreAuthorize("hasRole('AUTH')")
	@PatchMapping("/private/auth/update/email/{estimateId}")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> updateEstimateEmailByVerification(
			@AuthenticationPrincipal AuthDetails authDetails, @PathVariable("estimateId") int encodedEstimateId,
			@ClientSpecific("X-Verification-Email-Token") String verificationEmailToken, @RequestParam int version,
			HttpServletRequest req, HttpServletResponse res) {

		Estimate estimate = estimateService.updateEstimatePhoneByVerificationEmail(encodedEstimateId,
				verificationEmailToken, version);

		log.debug("* Estimate: [{}]", estimate);

		CookieUtil.deleteCookie(req, res, "X-Verification-Email-Token");

		return ResponseEntity
				.ok(apiResponseFactory.success(UPDATE_SUCCESS, EstimateResponseDto.fromEntity(estimate, obfuscator)));
	}

//  AUTH 토큰 통해 견적서 삭제
	@PreAuthorize("hasRole('AUTH')")
	@DeleteMapping("/private/auth/{estimateId}")
	public ResponseEntity<ApiResponseDto<Void>> deleteEstimateByAuth(@AuthenticationPrincipal AuthDetails authDetails,
			@PathVariable("estimateId") int encodedEstimateId, @RequestParam int version) {

		estimateService.deleteEstimateByAuthRecipient(encodedEstimateId, authDetails.getRecipient(), version);

		return ResponseEntity.ok(apiResponseFactory.success(DELETE_SUCCESS));
	}

}
