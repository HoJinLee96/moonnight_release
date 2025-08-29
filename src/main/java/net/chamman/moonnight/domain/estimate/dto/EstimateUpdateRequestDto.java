package net.chamman.moonnight.domain.estimate.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import net.chamman.moonnight.domain.estimate.Estimate.CleaningService;

public record EstimateUpdateRequestDto(
	    @NotBlank(message = "validation.user.name.required")
	    @Pattern(regexp = "^[가-힣a-zA-Z0-9\\s-]+$", message = "validation.user.name.invalid")
	    @Size(min = 2, max = 20, message = "validation.user.name.length")
	    String name,
	    
	    @Size(max = 5, message = "validation.address.postcode.length")
	    String postcode,
	    
	    @NotBlank(message = "validation.address.main_address.required")
	    @Size(max = 250, message = "validation.address.main_address.length")
	    String mainAddress,
	    
	    @Size(max = 250, message = "validation.address.detail_address.length")
	    String detailAddress,
	    
	    @NotNull(message = "validation.estimate.cleaning_service.required")
	    CleaningService cleaningService,
	    
	    @Size(max = 5000, message = "validation.estimate.content.length")
	    String content,
	    
	    List<String> deletedImagesPath,
	    
	    int version
	    ) {

}
