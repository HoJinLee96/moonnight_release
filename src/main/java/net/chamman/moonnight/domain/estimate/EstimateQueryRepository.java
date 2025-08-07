package net.chamman.moonnight.domain.estimate;


import org.springframework.data.domain.Pageable;

import net.chamman.moonnight.domain.estimate.dto.EstimateSearchRequestDto;
import net.chamman.moonnight.domain.estimate.dto.EstimateSearchResponseDto;

public interface EstimateQueryRepository {
    EstimateSearchResponseDto searchEstimates(EstimateSearchRequestDto dto, Pageable pageable);
}
