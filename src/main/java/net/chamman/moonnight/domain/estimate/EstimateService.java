package net.chamman.moonnight.domain.estimate;

import static net.chamman.moonnight.global.exception.HttpStatusCode.AUTHORIZATION_FAILED;
import static net.chamman.moonnight.global.exception.HttpStatusCode.ESTIMATE_NOT_FOUND;
import static net.chamman.moonnight.global.exception.HttpStatusCode.ESTIMATE_STATUS_DELETE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.domain.estimate.Estimate.EstimateStatus;
import net.chamman.moonnight.domain.estimate.dto.EstimateRequestDto;
import net.chamman.moonnight.domain.estimate.dto.EstimateResponseDto;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.domain.user.UserRepository;
import net.chamman.moonnight.global.exception.ForbiddenException;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.global.exception.infra.s3.S3UploadException;
import net.chamman.moonnight.global.exception.status.StatusDeleteException;
import net.chamman.moonnight.global.notification.NotificationService;
import net.chamman.moonnight.global.util.LogMaskingUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil.MaskLevel;
import net.chamman.moonnight.infra.email.EmailSender;
import net.chamman.moonnight.infra.sms.SmsSender;
import net.chamman.moonnight.rate.limiter.RateLimitService;

@Service
@Slf4j
@RequiredArgsConstructor
public class EstimateService {
	
	private final EstimateRepository estimateRepository;
	private final UserRepository userRepository;
	private final EstimateImagaeService estimateImagaeService;
	private final NotificationService notificationService;
	private final RateLimitService rateLimitService;
	private final SmsSender smsSender;
	private final EmailSender emailSender;
	private final Obfuscator obfuscator;
	
	//1.로그인한 유저 조회 OAUTH, LOCAL
	//2.휴대폰 인증 유저 조회 AUTH
	//3.휴대포번호, 견적서번호 조회 GUEST
	
	/** OAUTH, LOCAL 견적서 등록
	 * @param estimateRequestDto
	 * @param images
	 * @param userId
	 * 
	 * @throws S3UploadException {@link EstimateImagaeService#uploadEstimateImages}
	 * 
	 * @return 등록된 견적서
	 */
	@Transactional
	public EstimateResponseDto registerEstimate(EstimateRequestDto estimateRequestDto, List<MultipartFile> images, Integer userId, String clientIp)  {
		
		rateLimitService.checkEstimateByIp(clientIp);
		
		List<String> imagesPath = null;
		
//		1. 이미지 S3 등록
		if (images != null && !images.isEmpty()) {
			imagesPath = estimateImagaeService.uploadEstimateImages(images, estimateRequestDto.phone());
		}
		
//		2. 견적서 DB 등록
		try {
			User user = userId != null ? userRepository.getReferenceById(userId) : null;
			Estimate estimate = estimateRequestDto.toEntity(user, imagesPath);
			estimate.setClientIp(clientIp);
			estimateRepository.save(estimate);
			
			// 견적 신청 성공 안내 발송
			String phone = estimate.getPhone();
			String email = estimate.getEmail();
			if(!phone.isBlank()) {
				sendEstimateInfoSms(phone, obfuscator.encode(estimate.getEstimateId())+"");
			} else if(!email.isBlank()) {
				sendEstimateInfoEmail(email, obfuscator.encode(estimate.getEstimateId())+"");
			}
			
			return EstimateResponseDto.fromEntity(estimate, obfuscator);
		} catch (Exception e) {
//			3. 견적서 DB 등록 실패시 등록했던 이미지 S3 삭제 
	        if (imagesPath != null && !imagesPath.isEmpty()) {
	            try {
	                log.debug("* 견적서 등록 중. DB 작업 실패로 인한 S3 이미지 롤백 시작. 삭제 대상 경로: [{}]", imagesPath);
	                estimateImagaeService.deleteEstimateImages(imagesPath);
	                log.debug("* S3 이미지 롤백 완료.");
	            } catch (Exception s3DeleteEx) {
	                log.error(" S3 이미지 롤백 중 심각한 오류 발생! 삭제 대상 경로: [{}]. 에러: [{}]", imagesPath, s3DeleteEx.getMessage(), s3DeleteEx);
	                notificationService.sendAdminAlert("서버 문제 발생 \n S3 이미지 롤백 중 심각한 오류 발생!");
	            }
	        }
	        throw e;
		}
	}
	
	/** 검증 없이 견적서 조회
	 * @param encodedEstimateId
	 * @throws NoSuchDataException {@link #getEstimateOrThrow} 찾을 수 없는 견적서
	 * @throws StatusDeleteException {@link #isDelete} 이미 삭제된 견적서
	 * @return 견적서 엔티티
	 */
	public Estimate getEstimateById(int encodedEstimateId) {
		Estimate estimate = estimateRepository.findById(obfuscator.decode(encodedEstimateId))
				.orElseThrow(() -> new NoSuchDataException(ESTIMATE_NOT_FOUND, "찾을 수 없는 견적서."));
		isDelete(estimate);
		
		return estimate;
	}

	/** OAUTH, LOCAL 견적서 리스트 조회
	 * @param userId
	 * @return 견적서 리스트
	 */
	public List<EstimateResponseDto> getMyAllEstimate(int userId) {
		return estimateRepository.findByUser_UserId(userId)
				.stream()
				.filter(e->e.getEstimateStatus()!=EstimateStatus.DELETE)
				.map(e->EstimateResponseDto.fromEntity(e, obfuscator))
				.collect(Collectors.toList());
	}
	
	/** OAUTH, LOCAL 견적서 조회
	 * @param encodedEstimateId
	 * @param userId
	 * @throws NoSuchDataException {@link #getAuthorizedEstimate} 찾을 수 없는 견적서
	 * @throws ForbiddenException {@link #getAuthorizedEstimate} 견적서 조회 권한 이상
	 * @throws StatusDeleteException {@link #getAuthorizedEstimate} 이미 삭제된 견적서
	 * @return 견적서
	 */
	public EstimateResponseDto getMyEstimateByEstimateId(int encodedEstimateId, int userId) {
		Estimate estimate = getAuthorizedEstimate(encodedEstimateId, userId);
		return EstimateResponseDto.fromEntity(estimate, obfuscator);
	}
	
	/** AUTH 견적서 리스트 조회
	 * @param phone
	 * @return 견적서 리스트
	 */
	public List<EstimateResponseDto> getAllEstimateByAuth(String recipient) {
		return estimateRepository.findByEmailOrPhone(recipient)
				.stream()
				.filter(e->e.getEstimateStatus()!=EstimateStatus.DELETE)
				.map(e->EstimateResponseDto.fromEntity(e, obfuscator))
				.collect(Collectors.toList());
	}
	
	/** AUTH 견적서 조회
	 * @param encodedEstimateId
	 * @param phone
	 * 
	 * @throws NoSuchDataException {@link #getAuthorizedEstimate(int, String)} 찾을 수 없는 견적서
	 * @throws ForbiddenException {@link #getAuthorizedEstimate(int, String)} 견적서 조회 권한 이상
	 * @throws StatusDeleteException {@link #getAuthorizedEstimate(int, String)} 이미 삭제된 견적서
	 * 
	 * @return 견적서
	 */
	public EstimateResponseDto getEstimateByEstimateIdAndAuthRecipient(int encodedEstimateId, String recipient) {
		
		Estimate estimate = getAuthorizedEstimate(encodedEstimateId, recipient);
		
		return EstimateResponseDto.fromEntity(estimate, obfuscator);
	}
	
	/** OAUTH, LOCAL 견적서 수정
	 * @param estimateId
	 * @param estimateRequestDto
	 * @param images
	 * @param userId
	 * @return
	 * @throws NoSuchDataException {@link #getAuthorizedEstimate} 찾을 수 없는 견적서
	 * @throws ForbiddenException {@link #getAuthorizedEstimate} 견적서 조회 권한 이상
	 * @throws StatusDeleteException {@link #getAuthorizedEstimate} 이미 삭제된 견적서
	 * 
	 * @throws S3UploadException {@link #setNewEstimateAndSave} AWS S3에 파일 업로드 중 오류 발생 시.
	 */
	public EstimateResponseDto updateMyEstimate(int estimateId, EstimateRequestDto estimateRequestDto, List<MultipartFile> images, int userId) {

		Estimate estimate = getAuthorizedEstimate(estimateId, userId);
		
		log.debug("* estimate: [{}]",estimate);
		log.debug("* estimateRequestDto: [{}]",estimateRequestDto);

		Estimate updatedEstimate = setNewEstimateAndSave(estimate, estimateRequestDto, images);

		return EstimateResponseDto.fromEntity(updatedEstimate, obfuscator);
	}
	
	/** AUTH 견적서 수정
	 * @param encodedEstimateId
	 * @param estimateRequestDto
	 * @param images
	 * @param phone
	 * @return
	 * @throws NoSuchDataException {@link #getAuthorizedEstimate(int, String)} 찾을 수 없는 견적서
	 * @throws ForbiddenException {@link #getAuthorizedEstimate(int, String)} 견적서 조회 권한 이상
	 * @throws StatusDeleteException {@link #getAuthorizedEstimate(int, String)} 이미 삭제된 견적서
	 * 
	 * @throws S3UploadException {@link #setNewEstimateAndSave} AWS S3에 파일 업로드 중 오류 발생 시.
	 */
	public EstimateResponseDto updateEstimateByAuthRecipient(int encodedEstimateId, EstimateRequestDto estimateRequestDto, List<MultipartFile> images, String recipient) {

		Estimate estimate = getAuthorizedEstimate(encodedEstimateId, recipient);

		Estimate updatedEstimate = setNewEstimateAndSave(estimate, estimateRequestDto, images);

		return EstimateResponseDto.fromEntity(updatedEstimate, obfuscator);
	}
	
	/** OAUTH, LOCAL 견적서 삭제
	 * @param estimateId
	 * @param userId
	 * @throws NoSuchDataException {@link #getAuthorizedEstimate} 찾을 수 없는 견적서
	 * @throws ForbiddenException {@link #getAuthorizedEstimate} 견적서 조회 권한 이상
	 * @throws StatusDeleteException {@link #getAuthorizedEstimate} 이미 삭제된 견적서
	 */
	@Transactional
	public void deleteMyEstimate(int estimateId, int userId) {
		Estimate estimate = getAuthorizedEstimate(estimateId, userId);
		estimate.setEstimateStatus(EstimateStatus.DELETE);
	}
	
	/** AUTH 견적서 삭제
	 * @param encodedEstimateId
	 * @param phone
	 * @throws NoSuchDataException {@link #getAuthorizedEstimate(int, String)} 찾을 수 없는 견적서
	 * @throws ForbiddenException {@link #getAuthorizedEstimate(int, String)} 견적서 조회 권한 이상
	 * @throws StatusDeleteException {@link #getAuthorizedEstimate(int, String)} 이미 삭제된 견적서
	 */
	@Transactional
	public void deleteEstimateByAuthRecipient(int encodedEstimateId, String recipient) {
		Estimate estimate = getAuthorizedEstimate(encodedEstimateId, recipient);
		estimate.setEstimateStatus(EstimateStatus.DELETE);
	}
	
	/** 기존 견적서 엔티티에 새로운 값 SET 및 SAVE
	 * @param estimate 기존 엔티티
	 * @param estimateRequestDto 수정 될 객체
	 * @param images 수정 될 이미지
	 * @return 업데이트 된 엔티티
	 * @throws IOException
	 * @throws S3UploadException {@link EstimateImagaeService#uploadEstimateImages} AWS S3에 파일 업로드 중 오류 발생 시.
	 */
	@Transactional
	private Estimate setNewEstimateAndSave(Estimate estimate, EstimateRequestDto estimateRequestDto, List<MultipartFile> images) {

		// 기존 DB에 저장되어있는 파일 경로
		List<String> imagesPath = (estimate.getImagesPath() != null) ? estimate.getImagesPath(): new ArrayList<>();
		
		// 기존 DB중 삭제된 파일 경로
		List<String> deletedImagesPath = estimateRequestDto.imagesPath();
		
		// 새롭게 업로드된 파일 경로
		List<String> newImagesPath = null;
		
//		1. 새로운 이미지 S3 등록 및 기존 이미지 경로에 추가
		if (images != null && !images.isEmpty()) {
			newImagesPath = estimateImagaeService.uploadEstimateImages(images, estimate.getEstimateId()+"");
			imagesPath.addAll(newImagesPath);
		}
		
		// DB에 저장될 최종 이미지 경로 : 새로운 이미지 + 기존 이미지 - 삭제된 이미지
		if (deletedImagesPath != null && !deletedImagesPath.isEmpty()) {
			imagesPath.removeAll(deletedImagesPath);
		}
		
		
//		2. 견적서 DB 업데이트
		try {
			estimate.setName(estimateRequestDto.name());
			estimate.setEmail(estimateRequestDto.email());
			estimate.setEmailAgree(estimateRequestDto.emailAgree());
			estimate.setPhoneAgree(estimateRequestDto.phoneAgree());
			estimate.setPostcode(estimateRequestDto.postcode());
			estimate.setMainAddress(estimateRequestDto.mainAddress());
			estimate.setDetailAddress(estimateRequestDto.detailAddress());
			estimate.setContent(estimateRequestDto.content());
			estimate.setCleaningService(estimateRequestDto.cleaningService());
			estimate.setImagesPath(imagesPath);
			estimateRepository.save(estimate);
		} catch (Exception e) {
//			견적서 DB 업데이트 실패시 S3 등록했던 새로운 이미지 삭제
	        if (newImagesPath != null && !newImagesPath.isEmpty()) {
	            try {
	                log.debug("* 견적서 업데이트 중. DB 작업 실패로 인한 S3 이미지 롤백 시작. 삭제 대상 경로: [{}]", newImagesPath);
	                estimateImagaeService.deleteEstimateImages(newImagesPath);
	                log.debug("* S3 이미지 롤백 완료.");
	            } catch (Exception s3DeleteEx) {
	                log.error("* 견적서 업데이트 중. S3 이미지 롤백 중 심각한 오류 발생! 삭제 대상 경로: [{}]. 에러: [{}]", newImagesPath, s3DeleteEx.getMessage(), s3DeleteEx);
	                notificationService.sendAdminAlert("서버 문제 발생\nS3 이미지 롤백 중 심각한 오류 발생!");
	            }
	        }
	        throw e;
		}
//		4. 기존 이미지 S3 삭제
		if (deletedImagesPath!= null && !deletedImagesPath.isEmpty()) {
			try {
				estimateImagaeService.deleteEstimateImages(deletedImagesPath);
			} catch (Exception s3DeleteEx) {
                log.error("견적서 업데이트 중. 기존 S3 이미지 삭제 중 오류 발생! 삭제 대상 경로: {}. 에러: {}", deletedImagesPath, s3DeleteEx.getMessage(), s3DeleteEx);
                notificationService.sendAdminAlert("견적서 업데이트 중. 기존 S3 이미지 삭제 중 오류 발생");
			}
		}
		
		return estimateRepository.getReferenceById(estimate.getEstimateId());
	}
	
	/** 견적서 조회 및 검증
	 * @param encodedEstimateId
	 * @param userId
	 * @return 견적서 엔티티
	 * 
	 * @throws NoSuchDataException {@link #getEstimateById} 찾을 수 없는 견적서
	 * @throws StatusDeleteException {@link #getEstimateById} 삭제된 견적서
	 * @throws ForbiddenException {@link #getAuthorizedEstimate} 견적서 조회 권한 이상
	 */
	private Estimate getAuthorizedEstimate(int encodedEstimateId, int userId) {
		
		Estimate estimate = getEstimateById(encodedEstimateId);
		
		if (estimate.getUser().getUserId() != userId) {
			throw new ForbiddenException(AUTHORIZATION_FAILED,"견적서 조회 권한 이상. address.getUser().getUserId(): "+estimate.getUser().getUserId()+"!= userId: "+userId);
		}
		
		return estimate;
	}
	
	/** 견적서 조회 및 검증
	 * @param encodedEstimateId
	 * @param recipient
	 * @return 견적서 엔티티
	 * 
	 * @throws NoSuchDataException {@link #getEstimateById} 찾을 수 없는 견적서
	 * @throws StatusDeleteException {@link #getEstimateById} 삭제된 견적서
	 * @throws ForbiddenException {@link #getAuthorizedEstimate(int, String)} 견적서 조회 권한 이상
	 */
	private Estimate getAuthorizedEstimate(int encodedEstimateId, String recipient) {
		
		Estimate estimate = getEstimateById(encodedEstimateId);
		String phone = estimate.getPhone();
		String email = estimate.getEmail();
		if (!Objects.equals(phone, recipient) && !Objects.equals(email, recipient)) {
			log.warn("견적서 조회 권한 이상. phone: {}, email: {}, recipient: {}",
					LogMaskingUtil.maskPhone(phone, MaskLevel.MEDIUM),
					LogMaskingUtil.maskEmail(email, MaskLevel.MEDIUM),
					LogMaskingUtil.maskRecipient(recipient, MaskLevel.MEDIUM)
					);
			throw new ForbiddenException(AUTHORIZATION_FAILED,"견적서 조회 권한 이상.");
		}
		
		return estimate;
	}

	/** 견적서 EstimateStatus.DELETE 검사
	 * @param estimate
	 * 
	 * @throws StatusDeleteException {@link #isDelete} 삭제된 견적서
	 */
	private void isDelete(Estimate estimate) {
		if (estimate.getEstimateStatus() == EstimateStatus.DELETE) {
			throw new StatusDeleteException(ESTIMATE_STATUS_DELETE,"삭제된 견적서.");
		}
	}
	
	/** 견적 신청 확인 문자 안내
	 * @param recipientPhone
	 * @param estimateId
	 */
	public void sendEstimateInfoSms(String recipientPhone, String estimateId) {
		String message = "[달밤청소 견적 신청]\n"+
				"[ 견적 번호 : " + estimateId + " ]\n"+
				"달밤청소 문의 주셔서 감사합니다.\n"+
				"빠른 시일 내에 연락 드리겠습니다.";
		
		try {
			int statusCode = smsSender.sendSms(recipientPhone, message);
			if((statusCode/100)!=2) {
				log.error("안내 문자 발송 실패. statusCode: {}, phone: {}", statusCode, recipientPhone);
			}
		} catch (Exception e) {
			log.error("안내 문자 발송 실패. phone: {}, e: {}", recipientPhone, e);
		}
	}
	
	/** 견적 신청 확인 이메일 안내
	 * @param recipientPhone
	 * @param estimateId
	 */
	public void sendEstimateInfoEmail(String recipientEmail, String estimateId) {
		String title = "달밤청소 견적서";
		String message = "[ 견적 번호 : " + estimateId + " ]\n"+
				"달밤청소 문의 주셔서 감사합니다.\n"+
				"빠른 시일 내에 연락 드리겠습니다.";
		
		try {
			int statusCode = emailSender.sendEmail(recipientEmail, title, message);

			if((statusCode/100)!=2) {
				log.error("안내 이메일 발송 실패. statusCode: {}, email: {}", statusCode, recipientEmail);
			}
		} catch (Exception e) {
			log.error("안내 이메일 발송 실패. email: {}, e: {}", recipientEmail, e);
		}
	}
	
}
