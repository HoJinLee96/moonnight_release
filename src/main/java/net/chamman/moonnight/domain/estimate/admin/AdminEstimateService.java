package net.chamman.moonnight.domain.estimate.admin;

import static net.chamman.moonnight.global.exception.HttpStatusCode.ESTIMATE_NOT_FOUND;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.domain.estimate.Estimate;
import net.chamman.moonnight.domain.estimate.Estimate.EstimateStatus;
import net.chamman.moonnight.domain.estimate.EstimateRepository;
import net.chamman.moonnight.domain.estimate.EstimateService;
import net.chamman.moonnight.domain.estimate.admin.dto.EstimateUpdateStatusRequestDto;
import net.chamman.moonnight.domain.estimate.dto.EstimateUpdateRequestDto;
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
	 * 
	 * @throws NoSuchDataException   {@link #getEstimateOrThrow} 찾을 수 없는 견적서
	 * @throws StatusDeleteException {@link #isDelete} 이미 삭제된 견적서
	 * 
	 * @return 견적서 엔티티
	 */
	public Estimate getEstimateById(int encodedEstimateId) {
		return estimateRepository.findById(obfuscator.decode(encodedEstimateId))
				.orElseThrow(() -> new NoSuchDataException(ESTIMATE_NOT_FOUND, "찾을 수 없는 견적서."));
	}

	@Transactional
	public Estimate updateEstimate(int encodedEstimateId, EstimateUpdateRequestDto dto, List<MultipartFile> images) {

		Estimate estimate = getEstimateById(encodedEstimateId);
		estimate.verifyVersion(dto.version());
		log.debug("* estimate: [{}], estimateRequestDto: [{}], images: [{}]", estimate, dto, images);
		estimateService.modifyNewEstimate(estimate, dto, images);

		return estimate;
	}

	@Transactional
	public Estimate updateEstimateStatus(EstimateUpdateStatusRequestDto dto) {

		Estimate estimate = getEstimateById(dto.estimateId());
		estimate.verifyVersion(dto.version());
		estimate.setEstimateStatus(dto.estimateStatus());

		return estimate;
	}

	@Transactional
	public List<Estimate> updateMultipleEstimateStatus(List<EstimateUpdateStatusRequestDto> dtoList) {
		List<Estimate> list = new ArrayList<>();
		for (EstimateUpdateStatusRequestDto dto : dtoList) {
			list.add(updateEstimateStatus(dto));
		}
		return list;
	}

	@Transactional
	public void deleteEstimate(int encodedEstimateId, int version) {
		Estimate estimate = getEstimateById(encodedEstimateId);
		estimate.verifyVersion(version);
		estimate.setEstimateStatus(EstimateStatus.DELETE);
	}

}
