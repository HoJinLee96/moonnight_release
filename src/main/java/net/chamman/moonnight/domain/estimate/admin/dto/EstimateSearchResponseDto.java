package net.chamman.moonnight.domain.estimate.admin.dto;

import java.util.List;

import net.chamman.moonnight.domain.estimate.dto.EstimateResponseDto;

public record EstimateSearchResponseDto(
		EstimateStatusCount estimateStatusCount,
		long estimateSearchCount,
		List<EstimateResponseDto> estimateResponseDto) {
}
