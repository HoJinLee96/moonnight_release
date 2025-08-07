package net.chamman.moonnight.domain.estimate.dto;

import java.util.List;

public record EstimateSearchResponseDto(
		EstimateStatusCount estimateStatusCount,
		long estimateSearchCount,
		List<EstimateResponseDto> estimateResponseDto) {
}
