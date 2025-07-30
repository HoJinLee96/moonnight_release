package net.chamman.moonnight.domain.estimate.simple;

import static net.chamman.moonnight.global.exception.HttpStatusCode.CREATE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.DELETE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.READ_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.READ_SUCCESS_NO_DATA;

import java.nio.file.AccessDeniedException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.domain.estimate.simple.dto.SimpleEstimateRequestDto;
import net.chamman.moonnight.domain.estimate.simple.dto.SimpleEstimateResponseDto;
import net.chamman.moonnight.global.annotation.ValidId;
import net.chamman.moonnight.global.context.RequestContextHolder;
import net.chamman.moonnight.global.exception.HttpStatusCode;
import net.chamman.moonnight.global.exception.IllegalRequestException;
import net.chamman.moonnight.global.security.principal.CustomUserDetails;
import net.chamman.moonnight.global.util.ApiResponseDto;
import net.chamman.moonnight.global.util.ApiResponseFactory;

@RestController
@RequestMapping("/api/spem")
@RequiredArgsConstructor
@Slf4j
public class SimpleEstimateController {

	private final SimpleEstimateService spemService;
	private final ApiResponseFactory apiResponseFactory;

	// SimpleEstimate = Spem
	// 1.로그인한 유저 조회 OAUTH, LOCAL
	// 2.휴대폰 인증 유저 조회 AUTH
	// 3.휴대포번호, 견적서번호 조회 GUEST

	@PermitAll
	@PostMapping("/public/register")
	public ResponseEntity<ApiResponseDto<SimpleEstimateResponseDto>> registerSimpleEstimate(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@Valid @RequestBody SimpleEstimateRequestDto simpleEstimateRequestDto, HttpServletRequest request) {

		String clientIp = RequestContextHolder.getContext().getClientIp();
		
		String trap = simpleEstimateRequestDto.trap();
		if (trap != null && !trap.isEmpty()) {
			log.warn("* 간편 견적 봇 감지 확인. clientIp: [{}], trap: [{}]",clientIp, trap);
			throw new IllegalRequestException(HttpStatusCode.ILLEGAL_REQUEST);
		}
		
		Integer userId = userDetails != null ? userDetails.getUserId() : null;

		SimpleEstimateResponseDto spemResponseDto = spemService.registerSpem(simpleEstimateRequestDto, clientIp,
				userId);

		return ResponseEntity.status(HttpStatus.OK).body(apiResponseFactory.success(CREATE_SUCCESS, spemResponseDto));
	}

//  1
	@PreAuthorize("hasRole('OAUTH') or hasRole('LOCAL')")
	@GetMapping("/private/user")
	public ResponseEntity<ApiResponseDto<List<SimpleEstimateResponseDto>>> getMyAllSimpleEstimateByUserId(
			@AuthenticationPrincipal CustomUserDetails userDetails) {

		List<SimpleEstimateResponseDto> list = spemService.getMyAllSpem(userDetails.getUserId());

		if (list == null || list.isEmpty() || list.size() == 0) {
			return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS_NO_DATA, null));
		}

		return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS, list));
	}

//  1
	@PreAuthorize("hasRole('OAUTH') or hasRole('LOCAL')")
	@GetMapping("/private/user/{spemId}")
	public ResponseEntity<ApiResponseDto<SimpleEstimateResponseDto>> getMySimpleEstimateByEstimateId(
			@AuthenticationPrincipal CustomUserDetails userDetails, @ValidId @PathVariable int spemId)
			throws AccessDeniedException {

		SimpleEstimateResponseDto spemResponseDto = spemService.getMySpemBySpemId(spemId, userDetails.getUserId());

		return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS, spemResponseDto));
	}

//  2
	@PreAuthorize("hasRole('AUTH')")
	@GetMapping("/private/auth")
	public ResponseEntity<ApiResponseDto<List<SimpleEstimateResponseDto>>> getAllSimpleEstimateByAuthPhone(
			@AuthenticationPrincipal CustomUserDetails userDetails) {

		List<SimpleEstimateResponseDto> list = spemService.getAllSpemByAuthPhone(userDetails.getUsername());

		if (list == null || list.isEmpty() || list.size() == 0) {
			return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS_NO_DATA, null));
		}

		return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS, list));
	}

//  2
	@PreAuthorize("hasRole('AUTH')")
	@GetMapping("/private/auth/{spemId}")
	public ResponseEntity<ApiResponseDto<SimpleEstimateResponseDto>> getSimpleEstimateByAuthPhone(
			@AuthenticationPrincipal CustomUserDetails userDetails, @ValidId @PathVariable int spemId)
			throws AccessDeniedException {

		SimpleEstimateResponseDto simpleEstimateResponseDto = spemService.getSpemBySpemIdAndAuthPhone(spemId,
				userDetails.getUsername());

		return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS, simpleEstimateResponseDto));
	}

//  3
//  @PostMapping("/public/guest")
//  public ResponseEntity<ApiResponseDto<SimpleEstimateResponseDto>> getEstimateByEstimateIdAndPhone(
//      @ValidPhone @RequestParam String phone,
//      @ValidId @RequestParam int spemId) throws AccessDeniedException {
//    
//    SimpleEstimateResponseDto spemResponseDto = spemService.getSpemBySpemIdAndPhone(spemId,phone);
//    
//    return ResponseEntity.ok(ApiResponseDto.of(READ_SUCCESS spemResponseDto));
//  }

	@PreAuthorize("hasRole('OAUTH') or hasRole('LOCAL')")
	@DeleteMapping("/private/{spemId}")
	public ResponseEntity<ApiResponseDto<Void>> deleteMySimpleEstimate(
			@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable int spemId)
			throws AccessDeniedException {

		spemService.deleteMySpem(spemId, userDetails.getUserId());

		return ResponseEntity.ok(apiResponseFactory.success(DELETE_SUCCESS));
	}

	@PreAuthorize("hasRole('AUTH')")
	@DeleteMapping("/private/auth/{spemId}")
	public ResponseEntity<ApiResponseDto<Void>> deleteSimpleEstimateByAuthPhone(
			@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable int spemId)
			throws AccessDeniedException {

		spemService.deleteSpemByAuth(spemId, userDetails.getUsername());

		return ResponseEntity.ok(apiResponseFactory.success(DELETE_SUCCESS));
	}
}
