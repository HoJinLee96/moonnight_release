package net.chamman.moonnight.domain.estimate.dto;

import java.time.LocalDate;

import net.chamman.moonnight.domain.estimate.Estimate.CleaningService;
import net.chamman.moonnight.domain.estimate.Estimate.EstimateStatus;

public record EstimateSearchRequestDto(
		EstimateStatus estimateStatus,
		CleaningService cleaningService,
		String searchWord,
		String addressWord,
		LocalDate startDate,
		LocalDate endDate
		) {
}
