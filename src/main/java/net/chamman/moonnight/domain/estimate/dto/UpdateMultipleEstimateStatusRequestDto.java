package net.chamman.moonnight.domain.estimate.dto;

import java.util.List;

import net.chamman.moonnight.domain.estimate.Estimate.EstimateStatus;

public record UpdateMultipleEstimateStatusRequestDto(
		List<Integer> estimateIds,
		EstimateStatus estimateStatus
		) {

}
