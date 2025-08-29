package net.chamman.moonnight.domain.estimate.admin.dto;

import net.chamman.moonnight.domain.estimate.Estimate.EstimateStatus;

public record EstimateUpdateStatusRequestDto(
		int estimateId,
		EstimateStatus estimateStatus,
		int version
		) {

}
