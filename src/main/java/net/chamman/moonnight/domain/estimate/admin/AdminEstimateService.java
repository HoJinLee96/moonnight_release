package net.chamman.moonnight.domain.estimate.admin;

import static net.chamman.moonnight.global.exception.HttpStatusCode.ESTIMATE_NOT_FOUND;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.domain.estimate.Estimate;
import net.chamman.moonnight.domain.estimate.EstimateRepository;
import net.chamman.moonnight.domain.estimate.EstimateService;
import net.chamman.moonnight.domain.estimate.Estimate.EstimateStatus;
import net.chamman.moonnight.domain.estimate.dto.EstimateRequestDto;
import net.chamman.moonnight.domain.estimate.dto.EstimateResponseDto;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.global.exception.status.StatusDeleteException;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminEstimateService {

	private final EstimateRepository estimateRepository;
	private final EstimateService estimateService;
	private final Obfuscator obfuscator;

	/**
	 * 검증 없이 견적서 조회
	 * 
	 * @param encodedEstimateId
	 * @throws NoSuchDataException   {@link #getEstimateOrThrow} 찾을 수 없는 견적서
	 * @throws StatusDeleteException {@link #isDelete} 이미 삭제된 견적서
	 * @return 견적서 엔티티
	 */
	public EstimateResponseDto getEstimateById(int encodedEstimateId) {
		Estimate estimate = estimateRepository.findById(obfuscator.decode(encodedEstimateId))
				.orElseThrow(() -> new NoSuchDataException(ESTIMATE_NOT_FOUND, "찾을 수 없는 견적서."));

		return EstimateResponseDto.fromEntity(estimate, obfuscator);
	}
	
	@Transactional
	public EstimateResponseDto updateEstimateStatus(int encodedEstimateId, EstimateStatus estimateStatus) {

		Estimate estimate = getEstimate(encodedEstimateId);
		estimate.setEstimateStatus(estimateStatus);

		return EstimateResponseDto.fromEntity(estimate, obfuscator);
	}
	
	@Transactional
	public void updateMultipleEstimateStatus(List<Integer> estimateIds, EstimateStatus estimateStatus) {
		for(int encodedEstimateId : estimateIds) {
			updateEstimateStatus(encodedEstimateId, estimateStatus);
		}
	}

	@Transactional
	public EstimateResponseDto updateEstimate(int encodedEstimateId, EstimateRequestDto estimateRequestDto,
			List<MultipartFile> images) {

		Estimate estimate = getEstimate(encodedEstimateId);
		log.debug("* estimate: [{}], estimateRequestDto: [{}], images: [{}]",estimate, estimateRequestDto, images);
		estimateService.setNewEstimateAndSave(estimate, estimateRequestDto, images);

		return EstimateResponseDto.fromEntity(estimate, obfuscator);
	}

	private Estimate getEstimate(int encodedEstimateId) {
		return estimateRepository.findById(obfuscator.decode(encodedEstimateId))
				.orElseThrow(() -> new NoSuchDataException(ESTIMATE_NOT_FOUND, "찾을 수 없는 견적서."));
	}
	
	@Transactional
	public void deleteEstimate(int encodedEstimateId) {
		Estimate estimate = getEstimate(encodedEstimateId);
		estimate.setEstimateStatus(EstimateStatus.DELETE);
	}

}
