package net.chamman.moonnight.domain.admin;

import static net.chamman.moonnight.global.exception.HttpStatusCode.EMAIL_ALREADY_EXISTS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.PHONE_ALREADY_EXISTS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.SIGNIN_FAILED;
import static net.chamman.moonnight.global.exception.HttpStatusCode.USER_NOT_FOUND;
import static net.chamman.moonnight.global.exception.HttpStatusCode.USER_STATUS_DELETE;
import static net.chamman.moonnight.global.exception.HttpStatusCode.USER_STATUS_STAY;
import static net.chamman.moonnight.global.exception.HttpStatusCode.USER_STATUS_STOP;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.adminSign.AdminSignService;
import net.chamman.moonnight.auth.adminSign.log.AdminSignLog.SignResult;
import net.chamman.moonnight.auth.adminSign.log.AdminSignLogService;
import net.chamman.moonnight.auth.token.TokenProvider;
import net.chamman.moonnight.auth.token.TokenProvider.TokenType;
import net.chamman.moonnight.auth.token.dto.FindAdminPwTokenDto;
import net.chamman.moonnight.auth.token.dto.VerificationEmailTokenDto;
import net.chamman.moonnight.auth.token.dto.VerificationPhoneTokenDto;
import net.chamman.moonnight.auth.verification.VerificationService;
import net.chamman.moonnight.domain.admin.Admin.AdminStatus;
import net.chamman.moonnight.domain.admin.dto.AdminResponseDto;
import net.chamman.moonnight.global.exception.MismatchPasswordException;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.global.exception.admin.DuplicationException;
import net.chamman.moonnight.global.exception.crypto.DecryptException;
import net.chamman.moonnight.global.exception.crypto.EncryptException;
import net.chamman.moonnight.global.exception.redis.RedisGetException;
import net.chamman.moonnight.global.exception.redis.RedisSetException;
import net.chamman.moonnight.global.exception.sign.TooManySignFailException;
import net.chamman.moonnight.global.exception.status.StatusDeleteException;
import net.chamman.moonnight.global.exception.status.StatusStayException;
import net.chamman.moonnight.global.exception.status.StatusStopException;
import net.chamman.moonnight.global.exception.token.IllegalTokenException;
import net.chamman.moonnight.global.exception.token.NoSuchTokenException;
import net.chamman.moonnight.global.exception.verification.NotVerifyException;
import net.chamman.moonnight.global.exception.verification.VerificationExpiredException;
import net.chamman.moonnight.global.util.LogMaskingUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil.MaskLevel;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminService {

	private final AdminRepository adminRepository;
	private final VerificationService verificationService;
	private final AdminSignLogService signLogService;
	private final TokenProvider tokenProvider;
	private final PasswordEncoder passwordEncoder;


	/**
	 * 유저 엔티티 조회
	 * 
	 * @param adminId
	 * @throws NoSuchDataException   {@link #getAdminByAdminId} 찾을 수 없는 유저
	 * @throws StatusStayException   {@link #validateStatus} 일시정지 유저
	 * @throws StatusStopException   {@link #validateStatus} 중지 유저
	 * @throws StatusDeleteException {@link #validateStatus} 탈퇴 유저
	 * @return
	 */
	public Admin getActiveAdminByAdminId(int adminId) {

		Admin admin = adminRepository.findById(adminId)
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND, "찾을 수 없는 유저."));
		validateStatus(admin);

		return admin;
	}

	/**
	 * 유저 엔티티 조회
	 * 
	 * @param email
	 * @throws NoSuchDataException {@link #getAdminByEmailAndValidate} 찾을 수 없는 유저
	 * @return
	 */
	public Admin getAdminByEmail(String email) {
		return adminRepository.findByEmail(email)
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND, "찾을 수 없는 유저."));
	}

	/**
	 * 유저 엔티티 조회
	 * 
	 * @param email
	 * @throws NoSuchDataException   {@link #getAdminByEmailAndValidate} 찾을 수 없는 유저
	 * @throws StatusStayException   {@link #validateStatus} 일시정지 유저
	 * @throws StatusStopException   {@link #validateStatus} 중지 유저
	 * @throws StatusDeleteException {@link #validateStatus} 탈퇴 유저
	 * @return
	 */
	public Admin getActiveAdminByEmail(String email) {

		Admin admin = adminRepository.findByEmail(email)
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND, "찾을 수 없는 유저."));
		validateStatus(admin);

		return admin;
	}

	/**
	 * 유저 엔티티 조회
	 * 
	 * @param adminProvider
	 * @param email
	 * @param phone
	 * @throws NoSuchDataException   {@link #getAdminByAdminProviderAndEmailAndPhone}
	 *                               찾을 수 없는 유저
	 * @throws StatusStayException   {@link #validateStatus} 일시정지 유저
	 * @throws StatusStopException   {@link #validateStatus} 중지 유저
	 * @throws StatusDeleteException {@link #validateStatus} 탈퇴 유저
	 * @return adminProvider, email, phone 일치하는 Admin 조회 및 status 검사
	 */
	public Admin getAdminByEmailAndPhone(String email, String phone) {

		Admin admin = adminRepository.findByEmailAndPhone(email, phone)
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND, "찾을 수 없는 유저."));

		return admin;
	}

	/**
	 * 유저 엔티티 조회
	 * 
	 * @param adminProvider
	 * @param phone
	 * @param verificationPhoneToken
	 * 
	 * @throws IllegalTokenException        {@link TokenProvider#getDecryptedTokenDto}
	 *                                      토큰 문자열 null 또는 비어있음
	 * @throws NoSuchTokenException         {@link TokenProvider#getDecryptedTokenDto}
	 *                                      Redis 일치하는 토큰 없음
	 * @throws DecryptException             {@link TokenProvider#getDecryptedTokenDto}
	 *                                      복호화 실패
	 * @throws RedisGetException            {@link TokenProvider#getDecryptedTokenDto}
	 *                                      Redis 조회 실패
	 * 
	 * @throws NoSuchDataException          {@link VerificationService#isVerify} DB
	 *                                      verificationId 일치하는 인증 요청 없음
	 * @throws VerificationExpiredException {@link VerificationService#isVerify} DB
	 *                                      미인증된 인증 요청(시관 초과된 인증)
	 * @throws NotVerifyException           {@link VerificationService#isVerify} DB
	 *                                      미인증된 인증 요청
	 * 
	 * @throws NoSuchDataException          {@link #getAdminByAdminProviderAndPhone}
	 *                                      찾을 수 없는 유저
	 * @throws StatusStayException          {@link #getAdminByAdminProviderAndPhone}
	 *                                      일시정지 유저
	 * @throws StatusStopException          {@link #getAdminByAdminProviderAndPhone}
	 *                                      중지 유저
	 * @throws StatusDeleteException        {@link #getAdminByAdminProviderAndPhone}
	 *                                      탈퇴 유저
	 * 
	 * @return 휴대폰 번호와 일치하는 Admin 엔티티
	 */
	public AdminResponseDto getAdminByVerifiedPhone(String phone, String verificationPhoneToken) {

		// 휴대폰 인증 검증
		VerificationPhoneTokenDto verificationPhoneTokenDto = tokenProvider
				.getDecryptedTokenDto(VerificationPhoneTokenDto.TOKENTYPE, verificationPhoneToken);
		verificationService.isVerify(verificationPhoneTokenDto.getIntVerificationId());
		verificationPhoneTokenDto.comparePhone(phone);

		Admin admin = adminRepository.findByPhoneAndAdminStatusNot(phone, AdminStatus.DELETE).orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND, "찾을 수 없는 유저."));

		tokenProvider.removeToken(TokenType.VERIFICATION_PHONE, verificationPhoneToken);
		return AdminResponseDto.fromEntity(admin);
	}

	/**
	 * 비밀번호 찾기 자격 검증
	 * 
	 * @param adminProvider
	 * @param email
	 * @param phone
	 * @param verificationPhoneToken
	 * 
	 * @throws IllegalTokenException        {@link TokenProvider#getDecryptedTokenDto}
	 *                                      토큰 문자열 null 또는 비어있음
	 * @throws NoSuchTokenException         {@link TokenProvider#getDecryptedTokenDto}
	 *                                      Redis 일치하는 토큰 없음
	 * @throws DecryptException             {@link TokenProvider#getDecryptedTokenDto}
	 *                                      복호화 실패
	 * @throws RedisGetException            {@link TokenProvider#getDecryptedTokenDto}
	 *                                      Redis 조회 실패
	 * 
	 * @throws NoSuchDataException          {@link VerificationService#isVerify} DB
	 *                                      verificationId 일치하는 인증 요청 없음.
	 * @throws VerificationExpiredException {@link VerificationService#isVerify} DB
	 *                                      미인증된 인증 요청(시관 초과된 인증).
	 * @throws NotVerifyException           {@link VerificationService#isVerify} DB
	 *                                      미인증된 인증 요청.
	 * 
	 * @throws NoSuchDataException          {@link #getAdminByAdminProviderAndEmailAndPhone}
	 *                                      찾을 수 없는 유저
	 * @throws StatusStayException          {@link #getAdminByAdminProviderAndEmailAndPhone}
	 *                                      일시정지 유저
	 * @throws StatusStopException          {@link #getAdminByAdminProviderAndEmailAndPhone}
	 *                                      중지 유저
	 * @throws StatusDeleteException        {@link #getAdminByAdminProviderAndEmailAndPhone}
	 *                                      탈퇴 유저
	 * 
	 * @throws EncryptException             {@link TokenProvider#createToken} 암호화 실패
	 * @throws RedisSetException            {@link TokenProvider#createToken} Redis
	 *                                      저장 실패
	 * 
	 * @return 비밀번호 변경 자격 토큰
	 */
	@SuppressWarnings("incomplete-switch")
	public String createFindPwTokenByVerifyPhone(String email, String phone,
			String verificationPhoneToken) {

		VerificationPhoneTokenDto verificationPhoneTokenDto = tokenProvider
				.getDecryptedTokenDto(VerificationPhoneTokenDto.TOKENTYPE, verificationPhoneToken);
		verificationService.isVerify(verificationPhoneTokenDto.getIntVerificationId());

		Admin admin = getAdminByEmailAndPhone(email, phone);
		switch (admin.getAdminStatus()) {
		case STOP -> throw new StatusStopException(USER_STATUS_STOP, "정지된 계정. admin.id: " + admin.getAdminId());
		case DELETE -> throw new StatusDeleteException(USER_STATUS_DELETE, "탈퇴한 계정. admin.id: " + admin.getAdminId());
		}
		
		String findPwToken = tokenProvider.createToken(new FindAdminPwTokenDto(admin.getAdminId() + "", email),
				FindAdminPwTokenDto.TOKENTYPE);

		tokenProvider.removeToken(TokenType.VERIFICATION_PHONE, verificationPhoneToken);

		return findPwToken;
	}

	/**
	 * 비밀번호 찾기 자격 검증
	 * 
	 * @param adminProvider
	 * @param email
	 * @param verificationEmailToken
	 * 
	 * @throws IllegalTokenException        {@link TokenProvider#getDecryptedTokenDto}
	 *                                      토큰 문자열 null 또는 비어있음
	 * @throws NoSuchTokenException         {@link TokenProvider#getDecryptedTokenDto}
	 *                                      Redis 일치하는 토큰 없음
	 * @throws DecryptException             {@link TokenProvider#getDecryptedTokenDto}
	 *                                      복호화 실패
	 * @throws RedisGetException            {@link TokenProvider#getDecryptedTokenDto}
	 *                                      Redis 조회 실패
	 * 
	 * @throws NoSuchDataException          {@link VerificationService#isVerify} DB
	 *                                      verificationId 일치하는 인증 요청 없음.
	 * @throws VerificationExpiredException {@link VerificationService#isVerify} DB
	 *                                      미인증된 인증 요청(시관 초과된 인증).
	 * @throws NotVerifyException           {@link VerificationService#isVerify} DB
	 *                                      미인증된 인증 요청.
	 * 
	 * @throws NoSuchDataException          {@link #getAdminByAdminProviderAndEmail}
	 *                                      찾을 수 없는 유저
	 * @throws StatusStayException          {@link #getAdminByAdminProviderAndEmail}
	 *                                      일시정지 유저
	 * @throws StatusStopException          {@link #getAdminByAdminProviderAndEmail}
	 *                                      중지 유저
	 * @throws StatusDeleteException        {@link #getAdminByAdminProviderAndEmail}
	 *                                      탈퇴 유저
	 * 
	 * @throws EncryptException             {@link TokenProvider#createToken} 암호화 실패
	 * @throws RedisSetException            {@link TokenProvider#createToken} Redis
	 *                                      저장 실패
	 * 
	 * @return 비밀번호 변경 자격 토큰
	 */
	@SuppressWarnings("incomplete-switch")
	public String createFindPwTokenByVerifyEmail(String email, String verificationEmailToken) {

		VerificationEmailTokenDto verificationEmailTokenDto = tokenProvider
				.getDecryptedTokenDto(VerificationEmailTokenDto.TOKENTYPE, verificationEmailToken);
		verificationService.isVerify(verificationEmailTokenDto.getIntVerificationId());

		Admin admin = getAdminByEmail(email);
		switch (admin.getAdminStatus()) {
		case STOP -> throw new StatusStopException(USER_STATUS_STOP, "정지된 계정. admin.id: " + admin.getAdminId());
		case DELETE -> throw new StatusDeleteException(USER_STATUS_DELETE, "탈퇴한 계정. admin.id: " + admin.getAdminId());
		}

		String findPwToken = tokenProvider.createToken(new FindAdminPwTokenDto(admin.getAdminId() + "", email),
				FindAdminPwTokenDto.TOKENTYPE);

		tokenProvider.removeToken(TokenType.VERIFICATION_EMAIL, verificationEmailToken);

		return findPwToken;
	}

	/**
	 * 비밀번호 변경
	 * 
	 * @param token        FindPwToken
	 * @param adminProvider
	 * @param newPassword
	 * @param ip
	 * @throws IllegalTokenException {@link TokenProvider#getDecryptedTokenDto} 토큰
	 *                               문자열 null 또는 비어있음
	 * @throws NoSuchTokenException  {@link TokenProvider#getDecryptedTokenDto}
	 *                               Redis 일치하는 토큰 없음
	 * @throws DecryptException      {@link TokenProvider#getDecryptedTokenDto} 복호화
	 *                               실패
	 * @throws RedisGetException     {@link TokenProvider#getDecryptedTokenDto}
	 *                               Redis 조회 실패
	 * 
	 * @throws NoSuchDataException   {@link #getAdminByAdminId} 찾을 수 없는 유저
	 * @throws StatusStayException   {@link #validateStatus} 일시정지 유저
	 * @throws StatusStopException   {@link #validateStatus} 중지 유저
	 * @throws StatusDeleteException {@link #validateStatus} 탈퇴 유저
	 */
	@SuppressWarnings("incomplete-switch")
	@Transactional
	public void updatePasswordByFindPwToken(String token, String newPassword, String clientIp) {

		FindAdminPwTokenDto findPwTokenDto = tokenProvider.getDecryptedTokenDto(FindAdminPwTokenDto.TOKENTYPE, token);

		Admin admin = adminRepository.findById(findPwTokenDto.getIntAdminId())
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND, "찾을 수 없는 유저."));

		switch (admin.getAdminStatus()) {
		case STOP -> throw new StatusStopException(USER_STATUS_STOP, "정지된 계정. admin.id: " + admin.getAdminId());
		case DELETE -> throw new StatusDeleteException(USER_STATUS_DELETE, "탈퇴한 계정. admin.id: " + admin.getAdminId());
		}
		
		admin.setPassword(passwordEncoder.encode(newPassword));
		admin.setAdminStatus(AdminStatus.ACTIVE);
		adminRepository.save(admin);

		// 로그인 실패기록에 비밀번호 변경으로 초기화 진행
		signLogService.signAdminAndFailLogResolve(admin, SignResult.UPDATE_PASSWORD, clientIp);

		tokenProvider.removeToken(TokenType.ACCESS_FINDPW, token);
	}

	/**
	 * 비밀번호 검증
	 * 
	 * @param adminId
	 * @param password
	 * 
	 * @throws TooManySignFailException  {@link AdminSignService#validatePassword} 비밀번호
	 *                                   실패 횟수 초과
	 * @throws MismatchPasswordException {@link AdminSignService#validatePassword} 비밀번호
	 *                                   불일치
	 * 
	 * @throws EncryptException          {@link TokenProvider#createToken} 암호화 실패
	 * @throws RedisSetException         {@link TokenProvider#createToken} Redis 저장
	 *                                   실패
	 */
	public void confirmPassword(int adminId, String password, String clientIp) {
		Admin admin = getActiveAdminByAdminId(adminId);
		validatePassword(admin, password, clientIp);
	}

	/**
	 * 휴대폰 번호 변경
	 * 
	 * @param adminProvider
	 * @param email
	 * @param phone
	 * @param token
	 * @throws IllegalTokenException        {@link TokenProvider#getDecryptedTokenDto}
	 *                                      토큰 문자열 null 또는 비어있음
	 * @throws NoSuchTokenException         {@link TokenProvider#getDecryptedTokenDto}
	 *                                      Redis 일치하는 토큰 없음
	 * @throws DecryptException             {@link TokenProvider#getDecryptedTokenDto}
	 *                                      복호화 실패
	 * @throws RedisGetException            {@link TokenProvider#getDecryptedTokenDto}
	 *                                      Redis 조회 실패
	 * 
	 * @throws NoSuchDataException          {@link VerificationService#isVerify} DB
	 *                                      verificationId 일치하는 인증 요청 없음
	 * @throws VerificationExpiredException {@link VerificationService#isVerify} DB
	 *                                      미인증된 인증 요청(시관 초과된 인증)
	 * @throws NotVerifyException           {@link VerificationService#isVerify} DB
	 *                                      미인증된 인증 요청
	 * 
	 * @throws NoSuchDataException          {@link #getAdminByAdminId} 찾을 수 없는 유저
	 * @throws StatusStayException          {@link #getAdminByAdminId} 일시정지 유저
	 * @throws StatusStopException          {@link #getAdminByAdminId} 중지 유저
	 * @throws StatusDeleteException        {@link #getAdminByAdminId} 탈퇴 유저
	 * @return 유저
	 */
	@Transactional
	public void updatePhoneByVerification(int adminId, String phone, String token, String clientIp) {

		VerificationPhoneTokenDto verificationPhoneTokenDto = tokenProvider
				.getDecryptedTokenDto(VerificationPhoneTokenDto.TOKENTYPE, token);
		verificationService.isVerify(verificationPhoneTokenDto.getIntVerificationId());

		Admin admin = getActiveAdminByAdminId(adminId);
		admin.setPhone(phone);
		adminRepository.save(admin);
		
		tokenProvider.removeToken(TokenType.VERIFICATION_PHONE, token);
	}
	
	/**
	 * [회원가입용] 이메일 중복 및 사용 가능 여부 검증
	 * 
	 * @param email 검증할 이메일
	 * @throws DuplicationException 이미 사용 중인 이메일일 경우 발생
	 */
	public void isEmailExistsForRegistration(String email) {
		log.debug("* 이메일 중복 검사. email: {}", LogMaskingUtil.maskEmail(email, MaskLevel.MEDIUM));

		Optional<Admin> existAdmin = adminRepository.findByEmailAndAdminStatusNot(email,AdminStatus.DELETE);
		if (existAdmin.isPresent()) {
			throw new DuplicationException(EMAIL_ALREADY_EXISTS);
		}
	}
	
	/**
	 * [비밀번호 찾기용] 이메일 존재 및 LOCAL 계정 여부 확인
	 * 
	 * @param email 확인할 이메일
	 * @throws AdminNotFoundException 해당 이메일의 유저가 존재하지 않을 경우
	 * @throws OAuthAccountException 해당 이메일이 소셜 계정일 경우
	 */
	public String isEmailExistsForFindPassword(String email) {
		log.debug("* 비밀번호 찾기 이메일 검증. email: {}", LogMaskingUtil.maskEmail(email, MaskLevel.MEDIUM));

		Admin admin = adminRepository.findByEmailAndAdminStatusNot(email, AdminStatus.DELETE)
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND,"가입되지 않은 이메일.")); 

		return admin.getPhone();
	}

	/**
	 * 유저 휴대폰 번호 중복 검사
	 * 
	 * @param phone
	 */
	public void isPhoneExists(String phone) {
		log.debug("* 휴대폰 중복 검사. phone: {}", LogMaskingUtil.maskPhone(phone, MaskLevel.MEDIUM));

		Optional<Admin> existAdminProvider = adminRepository.findByPhoneAndAdminStatusNot(phone,
				AdminStatus.DELETE);
		if (existAdminProvider.isPresent()) {
			throw new DuplicationException(PHONE_ALREADY_EXISTS);
		}
	}

	/**
	 * 비밀번호 검증 및 결과 `LoginLog` 기록
	 * 
	 * @param admin
	 * @param reqPassword
	 * @param ip
	 * @throws TooManySignFailException  {@link AdminSignLogService#validSignFailCount}
	 *                                   비밀번호 실패 횟수 초과
	 * @throws MismatchPasswordException {@link #validatePassword} 비밀번호 불일치
	 */
	public void validatePassword(Admin admin, String reqPassword, String clientIp) {
		if (!passwordEncoder.matches(reqPassword, admin.getPassword())) {
			signLogService.signAdmin(admin, SignResult.INVALID_PASSWORD, clientIp);
			int signFailCount;
			try {
				signFailCount = signLogService.validSignFailCount(admin.getAdminId() + "");
			} catch (TooManySignFailException e) {
				admin.setAdminStatus(AdminStatus.STAY);
				adminRepository.save(admin);
				log.warn("로그인 실패 10회로 계정 일시정지: email={}, ip={}", admin.getEmail(), clientIp);
				throw e;
			}
			throw new MismatchPasswordException(SIGNIN_FAILED, "비밀번호 불일치. 실패 횟수: " + signFailCount);
		} 
	}

	/**
	 * 유저 상태 검사
	 * 
	 * @param admin
	 * @throws StatusStayException    {@link #validateStatus}
	 * @throws StatusStopException    {@link #validateStatus}
	 * @throws StatusDeleteExceptions {@link #validateStatus}
	 */
	@SuppressWarnings("incomplete-switch")
	public static void validateStatus(Admin admin) {
		switch (admin.getAdminStatus()) {
		case STAY -> throw new StatusStayException(USER_STATUS_STAY, "일시 정지된 계정. admin.id: " + admin.getAdminId());
		case STOP -> throw new StatusStopException(USER_STATUS_STOP, "정지된 계정. admin.id: " + admin.getAdminId());
		case DELETE -> throw new StatusDeleteException(USER_STATUS_DELETE, "탈퇴한 계정. admin.id: " + admin.getAdminId());
		}
	}

	/**
	 * @param adminStatus
	 * @throws StatusStayException    {@link #validateStatus}
	 * @throws StatusStopException    {@link #validateStatus}
	 * @throws StatusDeleteExceptions {@link #validateStatus}
	 */
//	@SuppressWarnings("incomplete-switch")
//	private void validateStatus(AdminStatus adminStatus) {
//		switch (adminStatus) {
//		case STAY -> throw new StatusStayException(USER_STATUS_STAY,"일시 정지된 계정.");
//		case STOP -> throw new StatusStopException(USER_STATUS_STOP,"정지된 계정.");
//		case DELETE -> throw new StatusDeleteException(USER_STATUS_DELETE,"탈퇴한 계정.");
//		}
//	}

	/**
	 * Redis에 저장되어있는 Key의 Value와 Request 값 비교
	 * 
	 * @param requestValue
	 * @param redisValue
	 * @throws TokenValueMismatchException {@link #validateByReidsValue} Redis에
	 *                                     저장되어있는 Key의 Value와 Request 값 불일치.
	 */
//	private void validateByReidsValue(String requestValue, String redisValue) {
//		if(!Objects.equals(requestValue,redisValue)) {
//			throw new TokenValueMismatchException(TOKEN_ILLEGAL,"redis에 저장되어있는 key의 value와 request 값 불일치. 입력값: {"+requestValue+"} != 저장값: {"+redisValue+"}.");
//		}
//	}

}