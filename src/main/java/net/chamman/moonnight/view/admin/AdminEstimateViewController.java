package net.chamman.moonnight.view.admin;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.domain.estimate.Estimate.CleaningService;
import net.chamman.moonnight.domain.estimate.Estimate.EstimateStatus;
import net.chamman.moonnight.view.EstimateViewController.CleaningServiceDto;

@Controller
@RequiredArgsConstructor
public class AdminEstimateViewController {
	
	@GetMapping("/admin/estimate")
	public String estimateView(Model model) {
		
		List<CleaningService> cleaningServiceList = Arrays.stream(CleaningService.values())
				.collect(Collectors.toList());
	    Map<String, String> estimateStatuses = Arrays.stream(EstimateStatus.values())
	            .filter(status -> status != EstimateStatus.ALL) // 네가 만든 isDisplayable() 재활용!
	            .collect(Collectors.toMap(Enum::name, EstimateStatus::getLabel));
	    
		model.addAttribute("estimateStatuses", estimateStatuses);
		model.addAttribute("cleaningServiceList", cleaningServiceList);
		List<CleaningServiceDto> cleaningServices = Arrays.stream(CleaningService.values())
				.map(e -> new CleaningServiceDto(e.name(), e.getLabel())).toList();

		model.addAttribute("cleaningServices", cleaningServices);
		
		return "admin/estimate/adminEstimate";
	}
}
