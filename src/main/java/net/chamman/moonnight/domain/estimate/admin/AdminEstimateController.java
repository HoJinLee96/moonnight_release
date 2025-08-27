package net.chamman.moonnight.domain.estimate.admin;

import static net.chamman.moonnight.global.exception.HttpStatusCode.DELETE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.READ_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.UPDATE_SUCCESS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.domain.estimate.Estimate;
import net.chamman.moonnight.domain.estimate.EstimateQueryRepository;
import net.chamman.moonnight.domain.estimate.admin.dto.EstimateSearchRequestDto;
import net.chamman.moonnight.domain.estimate.admin.dto.EstimateSearchResponseDto;
import net.chamman.moonnight.domain.estimate.admin.dto.EstimateUpdateStatusRequestDto;
import net.chamman.moonnight.domain.estimate.dto.EstimateResponseDto;
import net.chamman.moonnight.domain.estimate.dto.EstimateUpdateRequestDto;
import net.chamman.moonnight.global.annotation.ImageConstraint;
import net.chamman.moonnight.global.security.principal.CustomAdminDetails;
import net.chamman.moonnight.global.util.ApiResponseDto;
import net.chamman.moonnight.global.util.ApiResponseFactory;

@RestController
@RequestMapping("/api/admin/estimate")
@RequiredArgsConstructor
@Slf4j
public class AdminEstimateController {

	private final EstimateQueryRepository estimateQueryRepository;
	private final AdminEstimateService adminEstimateService;
	private final ApiResponseFactory apiResponseFactory;
	private final Obfuscator obfuscator;

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/private/search")
	public ResponseEntity<ApiResponseDto<EstimateSearchResponseDto>> estimateSearch(
			@RequestBody EstimateSearchRequestDto estimateSearchRequestDto, Pageable pageable, HttpServletRequest req,
			HttpServletResponse res) {

		EstimateSearchResponseDto list = estimateQueryRepository.searchEstimates(estimateSearchRequestDto, pageable);

		return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS, list));
	}

	@GetMapping("/private/{id}")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> getEstimateDetails(@PathVariable Integer id) {
		Estimate estimate = adminEstimateService.getEstimateById(id);
		return ResponseEntity
				.ok(apiResponseFactory.success(READ_SUCCESS, EstimateResponseDto.fromEntity(estimate, obfuscator)));
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PatchMapping("/private/update/{estimateId}")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> updateEstimateByAdmin(
			@AuthenticationPrincipal CustomAdminDetails customAdminDetails,
			@PathVariable("estimateId") int encodedEstimateId,
			@Valid @RequestPart EstimateUpdateRequestDto dto,
			@ImageConstraint @RequestPart(value = "images", required = false) List<MultipartFile> images,
			HttpServletRequest request) throws IOException {

		Estimate estimate = adminEstimateService.updateEstimate(encodedEstimateId, dto, images);

		return ResponseEntity
				.ok(apiResponseFactory.success(UPDATE_SUCCESS, EstimateResponseDto.fromEntity(estimate, obfuscator)));
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PatchMapping("/private/update/status")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> updateEstimateStatusByAdmin(
			@AuthenticationPrincipal CustomAdminDetails customAdminDetails,
			@RequestBody EstimateUpdateStatusRequestDto dto, HttpServletRequest request) {

		Estimate estimate = adminEstimateService.updateEstimateStatus(dto);

		return ResponseEntity
				.ok(apiResponseFactory.success(UPDATE_SUCCESS, EstimateResponseDto.fromEntity(estimate, obfuscator)));
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PatchMapping("/private/update/status/bulk")
	public ResponseEntity<ApiResponseDto<List<EstimateResponseDto>>> updateMultipleEstimateStatus(
			@AuthenticationPrincipal CustomAdminDetails customAdminDetails,
			@RequestBody List<EstimateUpdateStatusRequestDto> dtoList, HttpServletRequest request) {

		log.debug("* List<EstimateUpdateStatusRequestDto>: [{}]", dtoList);

		List<Estimate> list = adminEstimateService.updateMultipleEstimateStatus(dtoList);
		List<EstimateResponseDto> body = new ArrayList<>();
		if (!list.isEmpty()) {
			body = list.stream().map(e -> EstimateResponseDto.fromEntity(e, obfuscator)).collect(Collectors.toList());
		}

		return ResponseEntity.ok(apiResponseFactory.success(UPDATE_SUCCESS, body));
	}

	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/private/{estimateId}")
	public ResponseEntity<ApiResponseDto<Void>> deleteEstimateByAdmin(
			@AuthenticationPrincipal CustomAdminDetails customAdminDetails,
			@PathVariable("estimateId") int encodedEstimateId, @RequestParam int version) {

		adminEstimateService.deleteEstimate(encodedEstimateId, version);

		return ResponseEntity.ok(apiResponseFactory.success(DELETE_SUCCESS));
	}
}
