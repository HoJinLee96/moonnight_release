package net.chamman.moonnight.domain.estimate;

import static net.chamman.moonnight.global.exception.HttpStatusCode.DELETE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.READ_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.UPDATE_SUCCESS;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Pageable;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.domain.estimate.Estimate.EstimateStatus;
import net.chamman.moonnight.domain.estimate.dto.EstimateRequestDto;
import net.chamman.moonnight.domain.estimate.dto.EstimateResponseDto;
import net.chamman.moonnight.domain.estimate.dto.EstimateSearchRequestDto;
import net.chamman.moonnight.domain.estimate.dto.EstimateSearchResponseDto;
import net.chamman.moonnight.domain.estimate.dto.UpdateMultipleEstimateStatusRequestDto;
import net.chamman.moonnight.global.annotation.ImageConstraint;
import net.chamman.moonnight.global.security.principal.AuthDetails;
import net.chamman.moonnight.global.util.ApiResponseDto;
import net.chamman.moonnight.global.util.ApiResponseFactory;

@RestController
@RequestMapping("/api/admin/estimate")
@RequiredArgsConstructor
@Slf4j
public class AdminEstimateController {

	private final EstimateQueryRepository estimateQueryRepository;
	private final EstimateAdminService estimateAdminService;
	private final ApiResponseFactory apiResponseFactory;

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/private/search")
	public ResponseEntity<ApiResponseDto<EstimateSearchResponseDto>> estimateSearch(
			@RequestBody EstimateSearchRequestDto estimateSearchRequestDto,
			Pageable pageable,
			HttpServletRequest req,
			HttpServletResponse res) {

		EstimateSearchResponseDto list = estimateQueryRepository.searchEstimates(estimateSearchRequestDto, pageable);

		return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS, list));
	}
	
	@GetMapping("/private/{id}")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> getEstimateDetails(@PathVariable Integer id) {
		EstimateResponseDto estimateResponseDto = estimateAdminService.getEstimateById(id);
	    return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS, estimateResponseDto));
	}
	
	@PreAuthorize("hasRole('ADMIN')")
	@PatchMapping("/private/update/{estimateId}")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> updateEstimateByAdmin(
			@AuthenticationPrincipal AuthDetails authDetails,
			@PathVariable("estimateId") int encodedEstimateId,
			@Valid @RequestPart EstimateRequestDto estimateRequestDto,
			@ImageConstraint @RequestPart(value = "images", required = false) List<MultipartFile> images,
			HttpServletRequest request)
			throws IOException {
		
		EstimateResponseDto estimateResponseDto = estimateAdminService.updateEstimate(encodedEstimateId, estimateRequestDto, images);

		log.debug("* estimateResponseDto: [{}]",estimateResponseDto);
		return ResponseEntity.ok(apiResponseFactory.success(UPDATE_SUCCESS, estimateResponseDto));
	}
	
	@PreAuthorize("hasRole('ADMIN')")
	@PatchMapping("/private/update/status/{estimateId}")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> updateEstimateStatusByAdmin(
			@AuthenticationPrincipal AuthDetails authDetails,
			@PathVariable("estimateId") int encodedEstimateId,
			@RequestBody EstimateStatus estimateStatus,
			HttpServletRequest request) {
		
		EstimateResponseDto estimateResponseDto = estimateAdminService.updateEstimateStatus(encodedEstimateId, estimateStatus);

		log.debug("* estimateResponseDto: [{}]",estimateResponseDto);
		return ResponseEntity.ok(apiResponseFactory.success(UPDATE_SUCCESS, estimateResponseDto));
	}
	
	@PreAuthorize("hasRole('ADMIN')")
	@PatchMapping("/private/update/status/bulk")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> updateMultipleEstimateStatus(
			@AuthenticationPrincipal AuthDetails authDetails,
			@RequestBody UpdateMultipleEstimateStatusRequestDto dto,
			HttpServletRequest request) {
		
		log.debug("* UpdateMultipleEstimateStatusDto: [{}]", dto);
		
		estimateAdminService.updateMultipleEstimateStatus(dto.estimateIds(), dto.estimateStatus());
		
		return ResponseEntity.ok(apiResponseFactory.success(UPDATE_SUCCESS, null));
	}

	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/private/{estimateId}")
	public ResponseEntity<ApiResponseDto<Void>> deleteEstimateByAdmin(
			@AuthenticationPrincipal AuthDetails authDetails,
			@PathVariable("estimateId") int encodedEstimateId) {

		estimateAdminService.deleteEstimate(encodedEstimateId);

		return ResponseEntity.ok(apiResponseFactory.success(DELETE_SUCCESS));
	}
}
