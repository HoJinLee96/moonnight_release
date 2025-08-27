package net.chamman.moonnight.domain.estimate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import net.chamman.moonnight.domain.estimate.Estimate.CleaningService;

public record SimpleEstimateRequestDto(
		
		@NotBlank(message = "validation.user.phone.required")
		@Pattern(regexp = "^\\d{3,4}-\\d{3,4}-\\d{4}$", message = "validation.user.phone.invalid")
		@Size(max = 20, message = "{validation.user.phone.length}")
		String phone,
		
		@NotNull(message = "validation.estimate.cleaning_service.required")
		CleaningService cleaningService,
		
		@NotNull(message = "validation.estimate.region.required")
		String region,
		
		String trap
		
		)
{
	
}
