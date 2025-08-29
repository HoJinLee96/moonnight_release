package net.chamman.moonnight.domain.estimate.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.domain.estimate.Estimate;
import net.chamman.moonnight.domain.estimate.Estimate.CleaningService;
import net.chamman.moonnight.domain.estimate.Estimate.EstimateStatus;

@Builder
public record EstimateResponseDto(
     int estimateId,
     String name,
     String phone,
     String email,
     String postcode,
     String mainAddress,
     String detailAddress,
     String cleaningService,
     String content,
     List<String> images,
     EstimateStatus estimateStatus,
     int version,
     LocalDateTime createdAt,
     LocalDateTime updatedAt
    ) {
  
  public static EstimateResponseDto fromEntity(Estimate estimate, Obfuscator obfuscator) {
    return EstimateResponseDto.builder()
    .estimateId(obfuscator.encode(estimate.getEstimateId()))
    .name(estimate.getName())
    .phone(estimate.getPhone())
    .email(estimate.getEmail())
    .postcode(estimate.getPostcode())
    .mainAddress(estimate.getMainAddress())
    .detailAddress(estimate.getDetailAddress())
    .cleaningService(CleaningService.valueOf(estimate.getCleaningService()).getLabel())
    .content(estimate.getContent())
    .images(estimate.getImagesPath())
    .estimateStatus(estimate.getEstimateStatus())
    .version(estimate.getVersion())
    .createdAt(estimate.getCreatedAt())
    .updatedAt(estimate.getUpdatedAt())
    .build();
    
  }

}
