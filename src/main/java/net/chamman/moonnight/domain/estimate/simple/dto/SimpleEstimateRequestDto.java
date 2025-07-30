package net.chamman.moonnight.domain.estimate.simple.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import net.chamman.moonnight.domain.estimate.Estimate.CleaningService;
import net.chamman.moonnight.domain.estimate.Estimate.EstimateStatus;
import net.chamman.moonnight.domain.estimate.simple.SimpleEstimate;
import net.chamman.moonnight.domain.estimate.simple.SimpleEstimate.Region;
import net.chamman.moonnight.domain.user.User;

public record SimpleEstimateRequestDto(
		
		Integer estimateId,
		
		@NotBlank(message = "validation.user.phone.required")
		@Pattern(regexp = "^\\d{3,4}-\\d{3,4}-\\d{4}$", message = "validation.user.phone.invalid")
		@Size(max = 20, message = "{validation.user.phone.length}")
		String phone,
		
		@NotNull(message = "validation.estimate.cleaning_service.required")
		CleaningService cleaningService,
		
		@NotNull(message = "validation.estimate.region.required")
		Region region,
		
		String trap
		
		)
{
	public SimpleEstimate toEntity(User user, String clientIp) {
		return SimpleEstimate.builder()
				.user(user)
				.phone(phone)
				.cleaningService(cleaningService)
				.region(region)
				.estimateStatus(EstimateStatus.RECEIVE)
				.clientIp(clientIp)
				.build();
	}
	
}
