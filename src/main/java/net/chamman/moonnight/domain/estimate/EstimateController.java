package net.chamman.moonnight.domain.estimate;

import static net.chamman.moonnight.global.exception.HttpStatusCode.CREATE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.DELETE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.READ_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.READ_SUCCESS_NO_DATA;
import static net.chamman.moonnight.global.exception.HttpStatusCode.UPDATE_SUCCESS;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.domain.estimate.dto.EstimateRequestDto;
import net.chamman.moonnight.domain.estimate.dto.EstimateResponseDto;
import net.chamman.moonnight.global.annotation.ImageConstraint;
import net.chamman.moonnight.global.context.CustomRequestContextHolder;
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

	@Operation(summary = "견적서 등록", description = "견적서 등록")
	@PostMapping("/public/register")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> registerEstimate(
			@Valid @RequestPart EstimateRequestDto estimateRequestDto,
			@Valid @ImageConstraint @RequestPart(required = false) List<MultipartFile> images,
			HttpServletRequest request ) throws IOException, URISyntaxException {

		log.debug("* 견적서 등록 estimateRequestDto: {}", estimateRequestDto.toString());
		if(images != null) {
			images.forEach(path -> log.debug("* 견적서 imagesPath: {}", path.getName()));
		}

		String clientIp = CustomRequestContextHolder.getClientIp();

		EstimateResponseDto estimateResponseDto = estimateService.registerEstimate(estimateRequestDto, images, clientIp);

		return ResponseEntity.status(HttpStatus.OK).body(apiResponseFactory.success(CREATE_SUCCESS, estimateResponseDto));
	}

//  AUTH 토큰 통해 견적서 전체 조회
	@PreAuthorize("hasRole('AUTH')")
	@GetMapping("/private/auth")
	public ResponseEntity<ApiResponseDto<List<EstimateResponseDto>>> getAllEstimateByAuth(
			@AuthenticationPrincipal AuthDetails authDetails,HttpServletRequest req, HttpServletResponse res) {

		List<EstimateResponseDto> list = estimateService.getEstimateResponseDtoListByAuth(authDetails.getRecipient());

		if (list == null || list.isEmpty() || list.size() == 0) {
			CookieUtil.deleteCookie(req, res, "X-Auth-Token");
			return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS_NO_DATA, null));
		}

		return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS, list));
	}

//  AUTH 토큰 통해 견적서 단건 조회
	@PreAuthorize("hasRole('AUTH')")
	@GetMapping("/private/auth/{estimateId}")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> getEstimateByAuth(
			@AuthenticationPrincipal AuthDetails authDetails,
			@PathVariable("estimateId") int encodedEstimateId) {

		EstimateResponseDto estimateResponseDto = estimateService.getEstimateResponseDtoByIdAndAuthRecipient(encodedEstimateId,
				authDetails.getRecipient());

		return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS, estimateResponseDto));
	}

//  비회원 견적서 조회
	@PostMapping("/public/guest")
	public ResponseEntity<?> getEstimateByGuest(
			@RequestParam("estimateId") int encodedEstimateId,
			@RequestParam String recipient) {

		EstimateResponseDto estimateResponseDto = estimateService.getEstimateResponseDtoByIdAndAuthRecipient(encodedEstimateId,
				recipient);

		return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS, estimateResponseDto));
	}

//  AUTH 토큰 통해 견적서 수정
	@PreAuthorize("hasRole('AUTH')")
	@PatchMapping("/private/auth/update/{estimateId}")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> updateEstimateByAuth(
			@AuthenticationPrincipal AuthDetails authDetails,
			@PathVariable("estimateId") int encodedEstimateId,
			@Valid @RequestPart EstimateRequestDto estimateRequestDto,
			@ImageConstraint @RequestPart(value = "images", required = false) List<MultipartFile> images,
			HttpServletRequest request)
			throws IOException {
		
		EstimateResponseDto estimateResponseDto = estimateService.updateEstimateByAuthRecipient(encodedEstimateId, estimateRequestDto, images,
				authDetails.getRecipient());

		return ResponseEntity.ok(apiResponseFactory.success(UPDATE_SUCCESS, estimateResponseDto));
	}

//  AUTH 토큰 통해 견적서 삭제
	@PreAuthorize("hasRole('AUTH')")
	@DeleteMapping("/private/auth/{estimateId}")
	public ResponseEntity<ApiResponseDto<Void>> deleteEstimateByAuth(
			@AuthenticationPrincipal AuthDetails authDetails,
			@PathVariable("estimateId") int encodedEstimateId) {

		estimateService.deleteEstimateByAuthRecipient(encodedEstimateId, authDetails.getRecipient());

		return ResponseEntity.ok(apiResponseFactory.success(DELETE_SUCCESS));
	}

}
