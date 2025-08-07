package net.chamman.moonnight.domain.estimate.simple;

import static net.chamman.moonnight.global.exception.HttpStatusCode.CREATE_SUCCESS;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.domain.estimate.EstimateService;
import net.chamman.moonnight.domain.estimate.dto.EstimateResponseDto;
import net.chamman.moonnight.global.context.CustomRequestContextHolder;
import net.chamman.moonnight.global.exception.HttpStatusCode;
import net.chamman.moonnight.global.exception.IllegalRequestException;
import net.chamman.moonnight.global.util.ApiResponseDto;
import net.chamman.moonnight.global.util.ApiResponseFactory;

@RestController
@RequestMapping("/api/spem")
@RequiredArgsConstructor
@Slf4j
public class SimpleEstimateController {

	private final EstimateService estimateService;
	private final ApiResponseFactory apiResponseFactory;

	@PermitAll
	@PostMapping("/public/register")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> registerSimpleEstimate(
			@Valid @RequestBody SimpleEstimateRequestDto simpleEstimateRequestDto, HttpServletRequest request) {

		String clientIp = CustomRequestContextHolder.getClientIp();
		
		log.debug("* 간편 견적 신청. clientIp: [{}]", clientIp);
		
		String trap = simpleEstimateRequestDto.trap();
		if (trap != null && !trap.isEmpty()) {
			log.warn("* 간편 견적 봇 감지 확인. clientIp: [{}], trap: [{}]",clientIp, trap);
			throw new IllegalRequestException(HttpStatusCode.ILLEGAL_REQUEST);
		}
		
		EstimateResponseDto estimateResponseDto = estimateService.registerSimpleEstimate(simpleEstimateRequestDto, clientIp);

		return ResponseEntity.status(HttpStatus.OK).body(apiResponseFactory.success(CREATE_SUCCESS, estimateResponseDto));
	}
//
//	@PreAuthorize("hasRole('AUTH')")
//	@GetMapping("/private/auth")
//	public ResponseEntity<ApiResponseDto<List<EstimateResponseDto>>> getAllSimpleEstimateByAuthPhone(
//			@AuthenticationPrincipal AuthDetails authDetails) {
//
//		List<EstimateResponseDto> list = spemService.getAllSpemByAuthPhone(userDetails.getUsername());
//
//		if (list == null || list.isEmpty() || list.size() == 0) {
//			return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS_NO_DATA, null));
//		}
//
//		return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS, list));
//	}
//
////  2
//	@PreAuthorize("hasRole('AUTH')")
//	@GetMapping("/private/auth/{spemId}")
//	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> getSimpleEstimateByAuthPhone(
//			@AuthenticationPrincipal AuthDetails authDetails, @ValidId @PathVariable int spemId)
//			throws AccessDeniedException {
//
//		EstimateResponseDto simpleEstimateResponseDto = spemService.getSpemBySpemIdAndAuthPhone(spemId,
//				userDetails.getUsername());
//
//		return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS, simpleEstimateResponseDto));
//	}
//
//	@PreAuthorize("hasRole('AUTH')")
//	@DeleteMapping("/private/auth/{spemId}")
//	public ResponseEntity<ApiResponseDto<Void>> deleteSimpleEstimateByAuthPhone(
//			@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable int spemId)
//			throws AccessDeniedException {
//
//		spemService.deleteSpemByAuth(spemId, userDetails.getUsername());
//
//		return ResponseEntity.ok(apiResponseFactory.success(DELETE_SUCCESS));
//	}
}
