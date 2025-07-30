package net.chamman.moonnight.domain.user;

import static net.chamman.moonnight.global.exception.HttpStatusCode.EMAIL_ALREADY_EXISTS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.PHONE_ALREADY_EXISTS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.SIGNIN_FAILED;
import static net.chamman.moonnight.global.exception.HttpStatusCode.USER_NOT_FOUND;
import static net.chamman.moonnight.global.exception.HttpStatusCode.USER_ONLT_LOCAL;
import static net.chamman.moonnight.global.exception.HttpStatusCode.USER_STATUS_DELETE;
import static net.chamman.moonnight.global.exception.HttpStatusCode.USER_STATUS_STAY;
import static net.chamman.moonnight.global.exception.HttpStatusCode.USER_STATUS_STOP;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.oauth.OAuthResponseDto;
import net.chamman.moonnight.auth.oauth.OAuthService;
import net.chamman.moonnight.auth.sign.SignService;
import net.chamman.moonnight.auth.sign.log.SignLog.SignResult;
import net.chamman.moonnight.auth.sign.log.SignLogService;
import net.chamman.moonnight.auth.token.TokenProvider;
import net.chamman.moonnight.auth.token.TokenProvider.TokenType;
import net.chamman.moonnight.auth.token.dto.FindPwTokenDto;
import net.chamman.moonnight.auth.token.dto.VerificationEmailTokenDto;
import net.chamman.moonnight.auth.token.dto.VerificationPhoneTokenDto;
import net.chamman.moonnight.auth.verification.VerificationService;
import net.chamman.moonnight.domain.user.User.UserProvider;
import net.chamman.moonnight.domain.user.User.UserStatus;
import net.chamman.moonnight.domain.user.dto.UserResponseDto;
import net.chamman.moonnight.global.exception.ForbiddenException;
import net.chamman.moonnight.global.exception.NoSuchDataException;
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
import net.chamman.moonnight.global.exception.user.DuplicationException;
import net.chamman.moonnight.global.exception.user.MismatchPasswordException;
import net.chamman.moonnight.global.exception.verification.NotVerifyException;
import net.chamman.moonnight.global.exception.verification.VerificationExpiredException;
import net.chamman.moonnight.global.util.LogMaskingUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil.MaskLevel;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

	private final OAuthService oauthService;
	private final UserRepository userRepository;
	private final VerificationService verificationService;
	private final SignLogService signLogService;
	private final TokenProvider tokenProvider;
	private final PasswordEncoder passwordEncoder;


	/**
	 * 유저 엔티티 조회
	 * 
	 * @param userId
	 * @throws NoSuchDataException   {@link #getUserByUserId} 찾을 수 없는 유저
	 * @throws StatusStayException   {@link #validateStatus} 일시정지 유저
	 * @throws StatusStopException   {@link #validateStatus} 중지 유저
	 * @throws StatusDeleteException {@link #validateStatus} 탈퇴 유저
	 * @return
	 */
	public User getActiveUserByUserId(int userId) {

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND, "찾을 수 없는 유저."));
		validateStatus(user);

		return user;
	}

	/**
	 * 유저 엔티티 조회
	 * 
	 * @param email
	 * @throws NoSuchDataException {@link #getUserByEmailAndValidate} 찾을 수 없는 유저
	 * @return
	 */
	public User getUserByEmail(String email) {
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND, "찾을 수 없는 유저."));
	}

	/**
	 * 유저 엔티티 조회
	 * 
	 * @param email
	 * @throws NoSuchDataException   {@link #getUserByEmailAndValidate} 찾을 수 없는 유저
	 * @throws StatusStayException   {@link #validateStatus} 일시정지 유저
	 * @throws StatusStopException   {@link #validateStatus} 중지 유저
	 * @throws StatusDeleteException {@link #validateStatus} 탈퇴 유저
	 * @return
	 */
	public User getActiveUserByEmail(String email) {

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND, "찾을 수 없는 유저."));
		validateStatus(user);

		return user;
	}

	/**
	 * 유저 엔티티 조회
	 * 
	 * @param userProvider
	 * @param email
	 * @param phone
	 * @throws NoSuchDataException   {@link #getUserByUserProviderAndEmailAndPhone}
	 *                               찾을 수 없는 유저
	 * @throws StatusStayException   {@link #validateStatus} 일시정지 유저
	 * @throws StatusStopException   {@link #validateStatus} 중지 유저
	 * @throws StatusDeleteException {@link #validateStatus} 탈퇴 유저
	 * @return userProvider, email, phone 일치하는 User 조회 및 status 검사
	 */
	public User getUserByUserProviderAndEmailAndPhone(UserProvider userProvider, String email, String phone) {

		User user = userRepository.findByUserProviderAndEmailAndPhone(userProvider, email, phone)
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND, "찾을 수 없는 유저."));

		return user;
	}

	/**
	 * 유저 엔티티 조회
	 * 
	 * @param userProvider
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
	 * @throws NoSuchDataException          {@link #getUserByUserProviderAndPhone}
	 *                                      찾을 수 없는 유저
	 * @throws StatusStayException          {@link #getUserByUserProviderAndPhone}
	 *                                      일시정지 유저
	 * @throws StatusStopException          {@link #getUserByUserProviderAndPhone}
	 *                                      중지 유저
	 * @throws StatusDeleteException        {@link #getUserByUserProviderAndPhone}
	 *                                      탈퇴 유저
	 * 
	 * @return 휴대폰 번호와 일치하는 User 엔티티
	 */
	public UserResponseDto getUserByVerifiedPhone(String phone, String verificationPhoneToken) {

		// 휴대폰 인증 검증
		VerificationPhoneTokenDto verificationPhoneTokenDto = tokenProvider
				.getDecryptedTokenDto(VerificationPhoneTokenDto.TOKENTYPE, verificationPhoneToken);
		verificationService.isVerify(verificationPhoneTokenDto.getIntVerificationId());
		verificationPhoneTokenDto.comparePhone(phone);

		User user = userRepository.findByPhone(phone).filter((a) -> a.getUserStatus() != UserStatus.DELETE)
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND, "찾을 수 없는 유저."));

		if (Objects.equals(user.getUserProvider(), UserProvider.OAUTH)) {
			List<OAuthResponseDto> list = oauthService.getOAuthByUser(user);
			return UserResponseDto.fromEntity(user, list);
		}

		tokenProvider.removeToken(TokenType.VERIFICATION_PHONE, verificationPhoneToken);
		return UserResponseDto.fromEntity(user, null);
	}

	/**
	 * 비밀번호 찾기 자격 검증
	 * 
	 * @param userProvider
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
	 * @throws NoSuchDataException          {@link #getUserByUserProviderAndEmailAndPhone}
	 *                                      찾을 수 없는 유저
	 * @throws StatusStayException          {@link #getUserByUserProviderAndEmailAndPhone}
	 *                                      일시정지 유저
	 * @throws StatusStopException          {@link #getUserByUserProviderAndEmailAndPhone}
	 *                                      중지 유저
	 * @throws StatusDeleteException        {@link #getUserByUserProviderAndEmailAndPhone}
	 *                                      탈퇴 유저
	 * 
	 * @throws EncryptException             {@link TokenProvider#createToken} 암호화 실패
	 * @throws RedisSetException            {@link TokenProvider#createToken} Redis
	 *                                      저장 실패
	 * 
	 * @return 비밀번호 변경 자격 토큰
	 */
	@SuppressWarnings("incomplete-switch")
	public String createFindPwTokenByVerifyPhone(UserProvider userProvider, String email, String phone,
			String verificationPhoneToken) {

		VerificationPhoneTokenDto verificationPhoneTokenDto = tokenProvider
				.getDecryptedTokenDto(VerificationPhoneTokenDto.TOKENTYPE, verificationPhoneToken);
		verificationService.isVerify(verificationPhoneTokenDto.getIntVerificationId());

		User user = getUserByUserProviderAndEmailAndPhone(userProvider, email, phone);
		switch (user.getUserStatus()) {
		case STOP -> throw new StatusStopException(USER_STATUS_STOP, "정지된 계정. user.id: " + user.getUserId());
		case DELETE -> throw new StatusDeleteException(USER_STATUS_DELETE, "탈퇴한 계정. user.id: " + user.getUserId());
		}
		
		String findPwToken = tokenProvider.createToken(new FindPwTokenDto(user.getUserId() + "", email),
				FindPwTokenDto.TOKENTYPE);

		tokenProvider.removeToken(TokenType.VERIFICATION_PHONE, verificationPhoneToken);

		return findPwToken;
	}

	/**
	 * 비밀번호 찾기 자격 검증
	 * 
	 * @param userProvider
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
	 * @throws NoSuchDataException          {@link #getUserByUserProviderAndEmail}
	 *                                      찾을 수 없는 유저
	 * @throws StatusStayException          {@link #getUserByUserProviderAndEmail}
	 *                                      일시정지 유저
	 * @throws StatusStopException          {@link #getUserByUserProviderAndEmail}
	 *                                      중지 유저
	 * @throws StatusDeleteException        {@link #getUserByUserProviderAndEmail}
	 *                                      탈퇴 유저
	 * 
	 * @throws EncryptException             {@link TokenProvider#createToken} 암호화 실패
	 * @throws RedisSetException            {@link TokenProvider#createToken} Redis
	 *                                      저장 실패
	 * 
	 * @return 비밀번호 변경 자격 토큰
	 */
	@SuppressWarnings("incomplete-switch")
	public String createFindPwTokenByVerifyEmail(UserProvider userProvider, String email,
			String verificationEmailToken) {

		VerificationEmailTokenDto verificationEmailTokenDto = tokenProvider
				.getDecryptedTokenDto(VerificationEmailTokenDto.TOKENTYPE, verificationEmailToken);
		verificationService.isVerify(verificationEmailTokenDto.getIntVerificationId());

		User user = getUserByEmail(email);
		switch (user.getUserStatus()) {
		case STOP -> throw new StatusStopException(USER_STATUS_STOP, "정지된 계정. user.id: " + user.getUserId());
		case DELETE -> throw new StatusDeleteException(USER_STATUS_DELETE, "탈퇴한 계정. user.id: " + user.getUserId());
		}

		String findPwToken = tokenProvider.createToken(new FindPwTokenDto(user.getUserId() + "", email),
				FindPwTokenDto.TOKENTYPE);

		tokenProvider.removeToken(TokenType.VERIFICATION_EMAIL, verificationEmailToken);

		return findPwToken;
	}

	/**
	 * 비밀번호 변경
	 * 
	 * @param token        FindPwToken
	 * @param userProvider
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
	 * @throws NoSuchDataException   {@link #getUserByUserId} 찾을 수 없는 유저
	 * @throws StatusStayException   {@link #validateStatus} 일시정지 유저
	 * @throws StatusStopException   {@link #validateStatus} 중지 유저
	 * @throws StatusDeleteException {@link #validateStatus} 탈퇴 유저
	 */
	@SuppressWarnings("incomplete-switch")
	@Transactional
	public void updatePasswordByFindPwToken(String token, String newPassword, String clientIp) {

		FindPwTokenDto findPwTokenDto = tokenProvider.getDecryptedTokenDto(FindPwTokenDto.TOKENTYPE, token);

		User user = userRepository.findById(findPwTokenDto.getIntUserId())
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND, "찾을 수 없는 유저."));

		switch (user.getUserStatus()) {
		case STOP -> throw new StatusStopException(USER_STATUS_STOP, "정지된 계정. user.id: " + user.getUserId());
		case DELETE -> throw new StatusDeleteException(USER_STATUS_DELETE, "탈퇴한 계정. user.id: " + user.getUserId());
		}
		
		user.setPassword(passwordEncoder.encode(newPassword));
		user.setUserStatus(UserStatus.ACTIVE);
		userRepository.save(user);

		// 로그인 실패기록에 비밀번호 변경으로 초기화 진행
		signLogService.signUserAndFailLogResolve(user, SignResult.UPDATE_PASSWORD, clientIp);

		tokenProvider.removeToken(TokenType.ACCESS_FINDPW, token);
	}

	/**
	 * 비밀번호 검증
	 * 
	 * @param userId
	 * @param password
	 * 
	 * @throws TooManySignFailException  {@link SignService#validatePassword} 비밀번호
	 *                                   실패 횟수 초과
	 * @throws MismatchPasswordException {@link SignService#validatePassword} 비밀번호
	 *                                   불일치
	 * 
	 * @throws EncryptException          {@link TokenProvider#createToken} 암호화 실패
	 * @throws RedisSetException         {@link TokenProvider#createToken} Redis 저장
	 *                                   실패
	 */
	public void confirmPassword(int userId, String password, String clientIp) {
		User user = getActiveUserByUserId(userId);
		validatePassword(user, password, clientIp);
	}

	/**
	 * 휴대폰 번호 변경
	 * 
	 * @param userProvider
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
	 * @throws NoSuchDataException          {@link #getUserByUserId} 찾을 수 없는 유저
	 * @throws StatusStayException          {@link #getUserByUserId} 일시정지 유저
	 * @throws StatusStopException          {@link #getUserByUserId} 중지 유저
	 * @throws StatusDeleteException        {@link #getUserByUserId} 탈퇴 유저
	 * @return 유저
	 */
	@Transactional
	public void updatePhoneByVerification(int userId, String phone, String token, String clientIp) {

		VerificationPhoneTokenDto verificationPhoneTokenDto = tokenProvider
				.getDecryptedTokenDto(VerificationPhoneTokenDto.TOKENTYPE, token);
		verificationService.isVerify(verificationPhoneTokenDto.getIntVerificationId());

		User user = getActiveUserByUserId(userId);
		user.setPhone(phone);
		userRepository.save(user);
		
		tokenProvider.removeToken(TokenType.VERIFICATION_PHONE, token);
	}
	
	@Transactional
	public void updateProfile(int userId, String name, String birth, boolean marketingReceivedStatus, String clientIp) {

		User user = getActiveUserByUserId(userId);
		user.setName(name);
		user.setBirth(birth);
		user.setMarketingReceivedStatus(marketingReceivedStatus);
		userRepository.save(user);
	}
	

	/**
	 * 이메일 복구 유저인지 신규가입 유저인지 검사
	 * 
	 * @param email
	 */
	public Optional<User> isEmailDeleted(String email) {
		log.debug("* 이메일 통해 복구 유저인지 신규가입 유저인지 검사. email: {}", LogMaskingUtil.maskEmail(email, MaskLevel.MEDIUM));

		String deletedEmail = email + "_deleted";
		return userRepository.findByEmailAndUserStatus(deletedEmail, UserStatus.DELETE);
	}

	/**
	 * [회원가입용] 이메일 중복 및 사용 가능 여부 검증
	 * 
	 * @param email 검증할 이메일
	 * @throws DuplicationException 이미 사용 중인 이메일일 경우 발생
	 */
	public void isEmailExistsForRegistration(String email) {
		log.debug("* 이메일 중복 검사. email: {}", LogMaskingUtil.maskEmail(email, MaskLevel.MEDIUM));

		Optional<UserProvider> existUserProvider = userRepository.findUserProviderByEmailAndUserStatusNot(email,
				UserStatus.DELETE);
		if (existUserProvider.isPresent()) {
			throw new DuplicationException(EMAIL_ALREADY_EXISTS, existUserProvider.get(), email);
		}
	}
	
	/**
	 * [비밀번호 찾기용] 이메일 존재 및 LOCAL 계정 여부 확인
	 * 
	 * @param email 확인할 이메일
	 * @throws UserNotFoundException 해당 이메일의 유저가 존재하지 않을 경우
	 * @throws OAuthAccountException 해당 이메일이 소셜 계정일 경우
	 */
	public String isEmailExistsForFindPassword(String email) {
		log.debug("* 비밀번호 찾기 이메일 검증. email: {}", LogMaskingUtil.maskEmail(email, MaskLevel.MEDIUM));

		User user = userRepository.findByEmailAndUserStatusNot(email, UserStatus.DELETE)
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND,"가입되지 않은 이메일.")); 

		if (user.getUserProvider() == UserProvider.OAUTH) {
			throw new ForbiddenException(USER_ONLT_LOCAL);
		}
		return user.getPhone();
	}

	/**
	 * 유저 휴대폰 번호 중복 검사
	 * 
	 * @param phone
	 */
	public void isPhoneExists(String phone) {
		log.debug("* 휴대폰 중복 검사. phone: {}", LogMaskingUtil.maskPhone(phone, MaskLevel.MEDIUM));

		Optional<UserProvider> existUserProvider = userRepository.findUserProviderByPhoneAndUserStatusNot(phone,
				UserStatus.DELETE);
		if (existUserProvider.isPresent()) {
			throw new DuplicationException(PHONE_ALREADY_EXISTS, existUserProvider.get(), phone);
		}
	}

	/**
	 * 비밀번호 검증 및 결과 `LoginLog` 기록
	 * 
	 * @param user
	 * @param reqPassword
	 * @param ip
	 * @throws TooManySignFailException  {@link SignLogService#validSignFailCount}
	 *                                   비밀번호 실패 횟수 초과
	 * @throws MismatchPasswordException {@link #validatePassword} 비밀번호 불일치
	 */
	public void validatePassword(User user, String reqPassword, String clientIp) {
		if (!passwordEncoder.matches(reqPassword, user.getPassword())) {
			signLogService.signUser(user, SignResult.INVALID_PASSWORD, clientIp);
			int signFailCount;
			try {
				signFailCount = signLogService.validSignFailCount(UserProvider.LOCAL, user.getUserId() + "");
			} catch (TooManySignFailException e) {
				user.setUserStatus(UserStatus.STAY);
				userRepository.save(user);
				log.warn("로그인 실패 10회로 계정 일시정지: email={}, ip={}", user.getEmail(), clientIp);
				throw e;
			}
			throw new MismatchPasswordException(SIGNIN_FAILED, "비밀번호 불일치. 실패 횟수: " + signFailCount);
		} 
	}

	/**
	 * 유저 상태 검사
	 * 
	 * @param user
	 * @throws StatusStayException    {@link #validateStatus}
	 * @throws StatusStopException    {@link #validateStatus}
	 * @throws StatusDeleteExceptions {@link #validateStatus}
	 */
	@SuppressWarnings("incomplete-switch")
	public static void validateStatus(User user) {
		switch (user.getUserStatus()) {
		case STAY -> throw new StatusStayException(USER_STATUS_STAY, "일시 정지된 계정. user.id: " + user.getUserId());
		case STOP -> throw new StatusStopException(USER_STATUS_STOP, "정지된 계정. user.id: " + user.getUserId());
		case DELETE -> throw new StatusDeleteException(USER_STATUS_DELETE, "탈퇴한 계정. user.id: " + user.getUserId());
		}
	}

	/**
	 * @param userStatus
	 * @throws StatusStayException    {@link #validateStatus}
	 * @throws StatusStopException    {@link #validateStatus}
	 * @throws StatusDeleteExceptions {@link #validateStatus}
	 */
//	@SuppressWarnings("incomplete-switch")
//	private void validateStatus(UserStatus userStatus) {
//		switch (userStatus) {
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