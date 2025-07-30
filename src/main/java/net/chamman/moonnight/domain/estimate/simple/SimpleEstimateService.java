package net.chamman.moonnight.domain.estimate.simple;

import static net.chamman.moonnight.global.exception.HttpStatusCode.AUTHORIZATION_FAILED;
import static net.chamman.moonnight.global.exception.HttpStatusCode.ESTIMATE_NOT_FOUND;
import static net.chamman.moonnight.global.exception.HttpStatusCode.ESTIMATE_STATUS_DELETE;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.domain.estimate.Estimate.EstimateStatus;
import net.chamman.moonnight.domain.estimate.simple.dto.SimpleEstimateRequestDto;
import net.chamman.moonnight.domain.estimate.simple.dto.SimpleEstimateResponseDto;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.domain.user.UserRepository;
import net.chamman.moonnight.global.exception.ForbiddenException;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.global.exception.status.StatusDeleteException;
import net.chamman.moonnight.rate.limiter.RateLimitService;

@Service
@RequiredArgsConstructor
public class SimpleEstimateService {
	
	private final UserRepository userRepository;
	private final SimpleEstimateRepository spemRepository;
	private final Obfuscator obfuscator;
	private final RateLimitService rateLimitService;
	
	// SimpleEstimate = Spem
	//1.로그인한 유저 조회 OAUTH, USER
	//2.휴대폰 인증 유저 조회 AUTH
	//3.휴대포번호, 견적서번호 조회 GUEST
	
	/** 간편 견적서 등록
	 * @param pemRequestDto
	 * @param clientIp
	 * @return 간편 견적서 응답 객체
	 */
	@Transactional
	public SimpleEstimateResponseDto registerSpem(SimpleEstimateRequestDto spemRequestDto, String clientIp, Integer userId) {

		rateLimitService.checkEstimateByIp(clientIp);
		
		User user = userId != null ? userRepository.getReferenceById(userId) : null;

		SimpleEstimate spem = spemRequestDto.toEntity(user, clientIp);
		spemRepository.save(spem);

		return SimpleEstimateResponseDto.fromEntity(spem, obfuscator);
	}
	
	/** OAUTH, LOCAL 간편 견적서 리스트 조회
	 * @param userId
	 * @return 간편 견적서 응답 객체 리스트 NullAble
	 */
	public List<SimpleEstimateResponseDto> getMyAllSpem(int userId) {
		return spemRepository.findByUser_UserId(userId)
				.stream()
				.filter(e->e.getEstimateStatus()!=EstimateStatus.DELETE)
				.map(e->SimpleEstimateResponseDto.fromEntity(e, obfuscator))
				.collect(Collectors.toList());
	}
	
	/** OAUTH, LOCAL 간편 견적서 조회
	 * @param encodedSpemId
	 * @param userId
	 * @return 간편 견적서 응답 객체
	 * 
	 * @throws NoSuchDataException {@link #getAuthorizedSpem} 찾을 수 없는 간편 견적서
	 * @throws StatusDeleteException {@link #getAuthorizedSpem} 삭제된 견적서
	 * @throws ForbiddenException {@link #getAuthorizedSpem} 견적서 조회 권한 이상
	 */
	public SimpleEstimateResponseDto getMySpemBySpemId(int encodedSpemId, int userId) {
		SimpleEstimate spem = getAuthorizedSpem(encodedSpemId, userId);
		return SimpleEstimateResponseDto.fromEntity(spem, obfuscator);
	}
	
	/** AUTH 간편 견적서 리스트 조회
	 * @param phone
	 * @return 간편 견적서 응답 객체 리스트 NullAble
	 */
	public List<SimpleEstimateResponseDto> getAllSpemByAuthPhone(String phone) {
//		휴대폰 인증 jwt자체를 신뢰하는 방식통해 아래 코드 주석 처리 진행
//		verificationService.validateVerify(phone);
		return spemRepository.findByPhone(phone)
				.stream()
				.filter(e->e.getEstimateStatus()!=EstimateStatus.DELETE)
				.map(e->SimpleEstimateResponseDto.fromEntity(e, obfuscator))
				.collect(Collectors.toList());
	}
	
	/** AUTH 간편 견적서 조회
	 * @param encodedSpemId
	 * @param phone
	 * @return 간편 견적서 응답 객체
	 * 
	 * @throws NoSuchDataException {@link #getAuthorizedSpem(int, String)} 찾을 수 없는 간편 견적서
	 * @throws StatusDeleteException {@link #getAuthorizedSpem(int, String)} 삭제된 견적서
	 * @throws ForbiddenException {@link #getAuthorizedSpem(int, String)} 견적서 조회 권한 이상
	 */
	public SimpleEstimateResponseDto getSpemBySpemIdAndAuthPhone(int encodedSpemId, String phone) {
//		휴대폰 인증 jwt자체를 신뢰하는 방식통해 아래 코드 주석 처리 진행
//		verificationService.validateVerify(phone);
		SimpleEstimate spem = getAuthorizedSpem(encodedSpemId, phone);
		return SimpleEstimateResponseDto.fromEntity(spem, obfuscator);
	}
	
//  3
//	public SimpleEstimateResponseDto getSpemBySpemIdAndPhone(int spemId, String phone) {
//		SimpleEstimate spem = spemRepository.findById(obfuscator.decode(spemId))
//				.filter(e->e.getPhone()==phone)
//				.orElseThrow(()->new NoSuchElementException("일치하는 견적서가 없습니다."));
//		
//		validateNotDeleted(spem);
//		
//		return SimpleEstimateResponseDto.fromEntity(spem, obfuscator);
//	}
	
	/** OAUTH, LOCAL 간편 견적서 삭제
	 * @param encodedSpemId
	 * @param userId
	 * 
	 * @throws NoSuchDataException {@link #getAuthorizedSpem} 찾을 수 없는 간편 견적서
	 * @throws StatusDeleteException {@link #getAuthorizedSpem} 삭제된 견적서
	 * @throws ForbiddenException {@link #getAuthorizedSpem} 견적서 조회 권한 이상
	 */
	@Transactional
	public void deleteMySpem(int encodedSpemId, int userId) {
		
		SimpleEstimate spem = getAuthorizedSpem(encodedSpemId, userId);
		
		spem.setEstimateStatus(EstimateStatus.DELETE);
	}
	
	/** AUTH 간편 견적서 삭제
	 * @param encodedSpemId
	 * @param phone
	 * 
	 * @throws NoSuchDataException {@link #getAuthorizedSpem(int, String)} 찾을 수 없는 간편 견적서
	 * @throws StatusDeleteException {@link #getAuthorizedSpem(int, String)} 삭제된 견적서
	 * @throws ForbiddenException {@link #getAuthorizedSpem(int, String)} 견적서 조회 권한 이상
	 */
	@Transactional
	public void deleteSpemByAuth(int encodedSpemId, String phone) {
		
		SimpleEstimate spem = getAuthorizedSpem(encodedSpemId, phone);
		
		spem.setEstimateStatus(EstimateStatus.DELETE);
	}
	
	/** 간편 견적서 조회
	 * @param spemId
	 * @return 간편 견적서 엔티티
	 * 
	 * @throws NoSuchDataException {@link #getSpemByEncodedSpemId} 찾을 수 없는 간편 견적서
	 * @throws StatusDeleteException {@link #isDelete} 삭제된 견적서
	 */
	public SimpleEstimate getSpemByEncodedSpemId(int encodedSpemId) {
		SimpleEstimate simpleEstimate = spemRepository.findById(obfuscator.decode(encodedSpemId))
				.orElseThrow(() -> new NoSuchDataException(ESTIMATE_NOT_FOUND,"찾을 수 없는 간편 견적서."));
		
		isDelete(simpleEstimate);
		
		return simpleEstimate;
	}
	
	/** 간편 견적서 조회 및 검증
	 * @param encodedSpemId 난수화된 간편 견적서
	 * @param userId 로그인 되어 있는 유저
	 * @return 간편 견적서 엔티티
	 * 
	 * @throws NoSuchDataException {@link #getSpemByEncodedSpemId} 찾을 수 없는 간편 견적서
	 * @throws StatusDeleteException {@link #getSpemByEncodedSpemId} 삭제된 견적서
	 * @throws ForbiddenException {@link #getAuthorizedSpem} 견적서 조회 권한 이상
	 */
	private SimpleEstimate getAuthorizedSpem(int encodedSpemId, int userId) {
		SimpleEstimate spem = getSpemByEncodedSpemId(encodedSpemId);
		
		if (spem.getUser().getUserId() != userId) {
			throw new ForbiddenException(AUTHORIZATION_FAILED,"간편 견적서 조회 권한 이상. address.getUser().getUserId(): "+spem.getUser().getUserId()+"!= userId: "+userId);
		}
		
		return spem;
	}
	
	/**
	 * @param encodedSpemId 난수화된 간편 견적서
	 * @param phone 간편 견적서에 등록되어 있는 휴대폰 번호
	 * @return 간편 견적서 엔티티
	 * 
	 * @throws NoSuchDataException {@link #getSpemByEncodedSpemId} 찾을 수 없는 간편 견적서
	 * @throws StatusDeleteException {@link #getSpemByEncodedSpemId} 삭제된 견적서
	 * @throws ForbiddenException {@link #getAuthorizedSpem(int, String)} 견적서 조회 권한 이상
	 */
	private SimpleEstimate getAuthorizedSpem(int encodedSpemId, String phone) {
		SimpleEstimate spem = getSpemByEncodedSpemId(encodedSpemId);
		
		if (!Objects.equals(spem.getPhone(), phone)) {
			throw new ForbiddenException(AUTHORIZATION_FAILED,"간편 견적서 조회 권한 이상. estimate.getUser().getPhone(): "+spem.getUser().getPhone()+"!= phone: "+phone);
		}
		
		return spem;
	}

	/** 간편 견적서 EstimateStatus.DELETE 검사
	 * @param spem
	 * 
	 * @throws StatusDeleteException {@link #isDelete} 삭제된 견적서
	 */
	private void isDelete(SimpleEstimate spem) {
		if(spem.getEstimateStatus()==EstimateStatus.DELETE) {
			throw new StatusDeleteException(ESTIMATE_STATUS_DELETE,"삭제된 간편 견적서.");
		}
	}
	
}
