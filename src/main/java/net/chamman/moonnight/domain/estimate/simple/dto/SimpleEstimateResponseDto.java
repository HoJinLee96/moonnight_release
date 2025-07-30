package net.chamman.moonnight.domain.estimate.simple.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.domain.estimate.Estimate.CleaningService;
import net.chamman.moonnight.domain.estimate.Estimate.EstimateStatus;
import net.chamman.moonnight.domain.estimate.simple.SimpleEstimate;
import net.chamman.moonnight.domain.estimate.simple.SimpleEstimate.Region;

@Builder
public record SimpleEstimateResponseDto(
		int simpleEstimateId,
		String phone,
		CleaningService cleaningService,
		Region region,
		EstimateStatus estimateStatus,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
		) {
	
	public static SimpleEstimateResponseDto fromEntity(SimpleEstimate simpleEstimate, Obfuscator obfuscator) {
		return SimpleEstimateResponseDto.builder()
				.simpleEstimateId(obfuscator.encode(simpleEstimate.getSimpleEstimateId()))
				.phone(simpleEstimate.getPhone())
				.cleaningService(simpleEstimate.getCleaningService())
				.region(simpleEstimate.getRegion())
				.estimateStatus(simpleEstimate.getEstimateStatus())
				.createdAt(simpleEstimate.getCreatedAt())
				.updatedAt(simpleEstimate.getUpdatedAt())
				.build();
	}
}
