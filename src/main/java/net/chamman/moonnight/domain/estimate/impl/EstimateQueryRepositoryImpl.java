package net.chamman.moonnight.domain.estimate.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.domain.estimate.Estimate;
import net.chamman.moonnight.domain.estimate.Estimate.CleaningService;
import net.chamman.moonnight.domain.estimate.Estimate.EstimateStatus;
import net.chamman.moonnight.domain.estimate.admin.dto.EstimateSearchRequestDto;
import net.chamman.moonnight.domain.estimate.admin.dto.EstimateSearchResponseDto;
import net.chamman.moonnight.domain.estimate.admin.dto.EstimateStatusCount;
import net.chamman.moonnight.domain.estimate.EstimateQueryRepository;
import net.chamman.moonnight.domain.estimate.QEstimate;
import net.chamman.moonnight.domain.estimate.dto.EstimateResponseDto;

@Slf4j
@Repository
@RequiredArgsConstructor
public class EstimateQueryRepositoryImpl implements EstimateQueryRepository {

	private final JPAQueryFactory queryFactory;
	private final Obfuscator obfuscator;
	private final QEstimate estimate = QEstimate.estimate;

	@Override
	public EstimateSearchResponseDto searchEstimates(EstimateSearchRequestDto dto, Pageable pageable) {

		log.debug("* 견적서 검색. EstimateSearchRequestDto: [{}], Pageable: [{}]", dto, pageable);

		// 1. 기간이 포함된 상태별 카운트 조회 (수정)
		EstimateStatusCount statusCount = getStatusCount(dto);

		// 2. 조건에 맞는 데이터 리스트 조회 (where(), Pageable 적용으로 수정)
		List<Estimate> estimates = queryFactory.selectFrom(estimate)
				.where(statusEq(dto.estimateStatus()), cleaningServiceEq(dto.cleaningService()),
						searchWordContains(dto.searchWord()), addressWordContains(dto.addressWord()),
						createdAtBetween(dto.startDate(), dto.endDate()))
				.orderBy(estimate.createdAt.desc()).offset(pageable.getOffset()).limit(pageable.getPageSize()).fetch();

		// 3. DTO로 변환
		List<EstimateResponseDto> estimateResponseDtos = estimates.stream()
				.map(e -> EstimateResponseDto.fromEntity(e, obfuscator)).collect(Collectors.toList());

		// 4. 조건에 맞는 데이터 전체 카운트 조회 (where() 적용으로 수정)
		long totalCount = queryFactory.select(estimate.count()).from(estimate)
				.where(statusEq(dto.estimateStatus()), cleaningServiceEq(dto.cleaningService()),
						searchWordContains(dto.searchWord()), addressWordContains(dto.addressWord()),
						createdAtBetween(dto.startDate(), dto.endDate()))
				.fetchOne();

		// 5. 최종 응답 DTO 생성 및 반환
		EstimateSearchResponseDto estimateSearchResponseDto = new EstimateSearchResponseDto(statusCount,
				(int) totalCount, estimateResponseDtos);
		log.debug("* 견적서 검색 결과. EstimateSearchResponseDto: [{}]", estimateSearchResponseDto);

		return estimateSearchResponseDto;
	}

	private EstimateStatusCount getStatusCount(EstimateSearchRequestDto dto) {

		// ★ GROUP BY 쿼리 실행 ★
		List<Tuple> result = queryFactory.select(estimate.estimateStatus, estimate.count()).from(estimate)
				.where(cleaningServiceEq(dto.cleaningService()), searchWordContains(dto.searchWord()),
						addressWordContains(dto.addressWord()), createdAtBetween(dto.startDate(), dto.endDate()))
				.groupBy(estimate.estimateStatus) // 상태별로 그룹화
				.fetch();

		// List<Tuple>을 Map으로 수동 변환
		Map<EstimateStatus, Long> counts = result.stream()
				.collect(Collectors.toMap(tuple -> tuple.get(estimate.estimateStatus), // 첫 번째 값(상태)을 Key로
						tuple -> tuple.get(estimate.count()) // 두 번째 값(카운트)을 Value로
				));

		// '전체' 카운트는 별도로 조회 (이건 그룹화할 수 없으므로)
		long allCount = queryFactory.select(estimate.count()).from(estimate)
				.where(cleaningServiceEq(dto.cleaningService()), searchWordContains(dto.searchWord()),
						addressWordContains(dto.addressWord()), createdAtBetween(dto.startDate(), dto.endDate()))
				.fetchOne();

		// Map에서 각 상태별 카운트를 안전하게 꺼내온다.
		// getOrDefault: 해당 키의 값이 없으면(카운트가 0이면) Null 대신 0L을 반환
		long receiveCount = counts.getOrDefault(EstimateStatus.RECEIVE, 0L);
		long inProgressCount = counts.getOrDefault(EstimateStatus.IN_PROGRESS, 0L);
		long completeCount = counts.getOrDefault(EstimateStatus.COMPLETE, 0L);
		long deleteCount = counts.getOrDefault(EstimateStatus.DELETE, 0L);

		return new EstimateStatusCount((int) allCount, (int) receiveCount, (int) inProgressCount, (int) completeCount,
				(int) deleteCount);
	}

	// === BooleanExpression으로 재사용 가능한 조건 메서드들 (변경 없음) ===

	private BooleanExpression statusEq(EstimateStatus status) {
		if (status == null || status == EstimateStatus.ALL) {
			return null;
		}
		return estimate.estimateStatus.eq(status);
	}

	private BooleanExpression cleaningServiceEq(CleaningService cleaningService) {
		return cleaningService != null ? estimate.cleaningService.eq(cleaningService.name()) : null;
	}

	private BooleanExpression searchWordContains(String searchWord) {
		if (!StringUtils.hasText(searchWord)) {
			return null;
		}

		// 이름(name) 또는 연락처(phone) 또는 이메일(email)에 검색어가 포함되는지 확인 (대소문자 무시)
		return estimate.name.containsIgnoreCase(searchWord).or(estimate.phone.containsIgnoreCase(searchWord))
				.or(estimate.email.containsIgnoreCase(searchWord));
	}

	private BooleanExpression addressWordContains(String addressWord) {
		return StringUtils.hasText(addressWord) ? estimate.mainAddress.containsIgnoreCase(addressWord) : null;
	}

	private BooleanExpression createdAtBetween(LocalDate startDate, LocalDate endDate) {
		if (startDate == null || endDate == null) {
			return null;
		}

		return estimate.createdAt.between(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
	}
}