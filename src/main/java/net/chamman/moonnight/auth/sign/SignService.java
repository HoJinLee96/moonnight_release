package net.chamman.moonnight.auth.sign;

import static net.chamman.moonnight.global.exception.HttpStatusCode.AUTHORIZATION_FAILED;
import static net.chamman.moonnight.global.exception.HttpStatusCode.EMAIL_ALREADY_EXISTS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.ILLEGAL_INPUT_VALUE;
import static net.chamman.moonnight.global.exception.HttpStatusCode.SIGNIN_FAILED;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.auth.oauth.CustomOAuth2User;
import net.chamman.moonnight.auth.oauth.OAuth;
import net.chamman.moonnight.auth.oauth.OAuth.OAuthStatus;
import net.chamman.moonnight.auth.oauth.OAuthRepository;
import net.chamman.moonnight.auth.oauth.OAuthService;
import net.chamman.moonnight.auth.sign.dto.SignInRequestDto;
import net.chamman.moonnight.auth.sign.dto.SignUpRequestDto;
import net.chamman.moonnight.auth.sign.dto.UnlinkOAuthRequestDto;
import net.chamman.moonnight.auth.sign.log.SignLog;
import net.chamman.moonnight.auth.sign.log.SignLog.SignResult;
import net.chamman.moonnight.auth.sign.log.SignLogService;
import net.chamman.moonnight.auth.token.JwtProvider;
import net.chamman.moonnight.auth.token.TokenProvider;
import net.chamman.moonnight.auth.token.TokenProvider.TokenType;
import net.chamman.moonnight.auth.token.dto.SignUpTokenDto;
import net.chamman.moonnight.auth.token.dto.VerificationEmailTokenDto;
import net.chamman.moonnight.auth.token.dto.VerificationPhoneTokenDto;
import net.chamman.moonnight.auth.verification.VerificationService;
import net.chamman.moonnight.domain.address.Address;
import net.chamman.moonnight.domain.address.AddressRepository;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.domain.user.User.UserProvider;
import net.chamman.moonnight.domain.user.User.UserStatus;
import net.chamman.moonnight.domain.user.UserRepository;
import net.chamman.moonnight.domain.user.UserService;
import net.chamman.moonnight.global.exception.ForbiddenException;
import net.chamman.moonnight.global.exception.IllegalRequestException;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.global.exception.crypto.DecryptException;
import net.chamman.moonnight.global.exception.crypto.EncryptException;
import net.chamman.moonnight.global.exception.jwt.CreateJwtException;
import net.chamman.moonnight.global.exception.jwt.IllegalJwtException;
import net.chamman.moonnight.global.exception.jwt.TimeOutJwtException;
import net.chamman.moonnight.global.exception.jwt.ValidateJwtException;
import net.chamman.moonnight.global.exception.redis.RedisGetException;
import net.chamman.moonnight.global.exception.redis.RedisSetException;
import net.chamman.moonnight.global.exception.sign.TooManySignFailException;
import net.chamman.moonnight.global.exception.status.StatusDeleteException;
import net.chamman.moonnight.global.exception.status.StatusStayException;
import net.chamman.moonnight.global.exception.status.StatusStopException;
import net.chamman.moonnight.global.exception.token.IllegalTokenException;
import net.chamman.moonnight.global.exception.token.NoSuchTokenException;
import net.chamman.moonnight.global.exception.token.TokenValueMismatchException;
import net.chamman.moonnight.global.exception.user.DuplicationException;
import net.chamman.moonnight.global.exception.user.MismatchPasswordException;
import net.chamman.moonnight.global.exception.verification.NotVerifyException;
import net.chamman.moonnight.global.exception.verification.VerificationExpiredException;
import net.chamman.moonnight.global.util.LogMaskingUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil.MaskLevel;

@Service
@Slf4j
@RequiredArgsConstructor
public class SignService {

	private final Obfuscator obfuscator;

	private final AddressRepository addressRepository;
	private final UserRepository userRepository;
	private final OAuthRepository oauthRepository;

	private final UserService userService;
	private final OAuthService oauthService;
	private final VerificationService verificationService;
	private final SignLogService signLogService;

	private final JwtProvider jwtProvider;
	private final TokenProvider tokenProvider;
	private final PasswordEncoder passwordEncoder;
	private final Map<UserProvider, String> roles = Map.of(UserProvider.LOCAL, "ROLE_LOCAL", UserProvider.OAUTH,
			"ROLE_OAUTH");

	/**
	 * LOCAL 회원가입 1차
	 * 
	 * @param email
	 * @param password
	 * @param valificationEmailToken
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
	 * @throws DuplicationException         {@link UserService#isEmailExists} 이메일 중복
	 * 
	 * @throws EncryptException             {@link TokenProvider#createToken} 암호화 실패
	 * @throws RedisSetException            {@link TokenProvider#createToken} Redis
	 *                                      저장 실패
	 * 
	 * @return
	 */
	@Transactional
	public String createSignUpToken(String email, String password, String valificationEmailToken) {
		log.debug("*회원 가입 1차 요청. email: [{}] with verificationEmailToken: [{}]",
				LogMaskingUtil.maskEmail(email, MaskLevel.MEDIUM),
				LogMaskingUtil.maskToken(valificationEmailToken, MaskLevel.MEDIUM));

		VerificationEmailTokenDto verificationEmailTokenDto = tokenProvider
				.getDecryptedTokenDto(VerificationEmailTokenDto.TOKENTYPE, valificationEmailToken);
		verificationService.isVerify(verificationEmailTokenDto.getIntVerificationId());
		if (!Objects.equals(verificationEmailTokenDto.getEmail(), email)) {
			throw new IllegalRequestException(ILLEGAL_INPUT_VALUE);
		}

		// 이메일 중복 검사
		userService.isEmailExistsForRegistration(email);

		String token = tokenProvider.createToken(new SignUpTokenDto(email, password), SignUpTokenDto.TOKENTYPE);

		tokenProvider.removeToken(TokenType.VERIFICATION_EMAIL, valificationEmailToken);

		return token;
	}

	/**
	 * LOCAL 회원가입 2차
	 * 
	 * @param userCreateRequestDto
	 * @param accessJoinToken        회원가입 1차 토큰
	 * @param verificationPhoneToken 휴대폰 인증 토큰
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
	 * @throws DuplicationException         {@link UserService#isPhoneExists} 휴대폰 중복
	 * 
	 * @return 회원 가입 유저 이름
	 */
	@Transactional
	public String signUpLocalUser(SignUpRequestDto signUpRequestDto, String accessSignUpToken,
			String verificationPhoneToken, String clientIp) {

		SignUpTokenDto signUpTokenDto = tokenProvider.getDecryptedTokenDto(SignUpTokenDto.TOKENTYPE, accessSignUpToken);

		VerificationPhoneTokenDto verificationPhoneTokenDto = tokenProvider
				.getDecryptedTokenDto(VerificationPhoneTokenDto.TOKENTYPE, verificationPhoneToken);
		verificationService.isVerify(verificationPhoneTokenDto.getIntVerificationId());
		if (!Objects.equals(signUpRequestDto.phone(), verificationPhoneTokenDto.getPhone())) {
			log.debug("* LOCAL 회원 가입 중 휴대폰 번호 입력 값 상이.");
			throw new IllegalRequestException(ILLEGAL_INPUT_VALUE);
		}

		userService.isPhoneExists(verificationPhoneTokenDto.getPhone());

		// 비밀번호 인코딩 후 저장
		String encodedPassoword = passwordEncoder.encode(signUpTokenDto.getRawPassword());

		User user = signUpRequestDto.toEntity();
		user.setEmail(signUpTokenDto.getEmail());
		user.setPassword(encodedPassoword);
		user.setPhone(verificationPhoneTokenDto.getPhone());
		user.setUserProvider(UserProvider.LOCAL);
		userRepository.save(user);

		Address address = signUpRequestDto.toAddressEntity(user);
		addressRepository.save(address);

		signLogService.signUser(user, SignResult.SIGNUP, clientIp);

		tokenProvider.removeToken(SignUpTokenDto.TOKENTYPE, accessSignUpToken);
		tokenProvider.removeToken(VerificationPhoneTokenDto.TOKENTYPE, verificationPhoneToken);
		removeDeletedUser(signUpTokenDto.getEmail()); // 지난 탈퇴 유저 데이터 삭제

		return signUpRequestDto.name();
	}

	/**
	 * OAUTH 회원 가입
	 * 
	 * @param signUpRequestDto
	 * @param signUpOAuthToken
	 * @param verificationPhoneToken
	 * @param clientIp
	 * @return
	 */
	@Transactional
	public String signUpOAuthUser(SignUpRequestDto signUpRequestDto, String signUpOAuthToken,
			String verificationPhoneToken, String clientIp) {

		CustomOAuth2User customOAuth2User = tokenProvider.getDecryptedTokenDto(CustomOAuth2User.TOKENTYPE,
				signUpOAuthToken);

		verifyPhoneAndCompare(signUpRequestDto.phone(), verificationPhoneToken);

		userService.isPhoneExists(signUpRequestDto.phone());

		User user = signUpRequestDto.toEntity();
		user.setEmail(customOAuth2User.getEmail());
		user.setPhone(signUpRequestDto.phone());
		user.setUserProvider(UserProvider.OAUTH);
		userRepository.save(user);

		Address address = signUpRequestDto.toAddressEntity(user);
		addressRepository.save(address);

		signLogService.signUser(user, SignResult.SIGNUP, clientIp);

		saveOAuth(user, customOAuth2User, clientIp);

		tokenProvider.removeToken(CustomOAuth2User.TOKENTYPE, signUpOAuthToken);
		tokenProvider.removeToken(VerificationPhoneTokenDto.TOKENTYPE, verificationPhoneToken);
		removeDeletedUser(customOAuth2User.getEmail()); // 지난 탈퇴 유저 데이터 삭제

		return signUpRequestDto.name();
	}

	/**
	 * 탈퇴 계정을 LOCAL로 계정 복원
	 * 
	 * @param signUpRequestDto
	 * @param accessSignUpToken
	 * @param verificationPhoneToken
	 * @param clientIp
	 * @return
	 */
	@Transactional
	public String signUpRestorationLocalUser(SignUpRequestDto signUpRequestDto, String accessSignUpToken,
			String verificationPhoneToken, String clientIp) {

		SignUpTokenDto signUpTokenDto = tokenProvider.getDecryptedTokenDto(SignUpTokenDto.TOKENTYPE, accessSignUpToken);

		// 휴대폰 인증 검증
		verifyPhoneAndCompare(signUpRequestDto.phone(), verificationPhoneToken);

		// 휴대폰 중복 검증
		userService.isPhoneExists(signUpRequestDto.phone());

		// 비밀번호 인코딩
		String encodedPassoword = passwordEncoder.encode(signUpTokenDto.getRawPassword());

		// User 수정, Address 등록
		User user = userService.getUserByEmail(signUpTokenDto.getEmail() + "_deleted");
		updateUserWithNewDetails(user, signUpRequestDto, UserProvider.LOCAL, encodedPassoword);

		signLogService.signUser(user, SignResult.RESTORATION, clientIp);

		tokenProvider.removeToken(SignUpTokenDto.TOKENTYPE, accessSignUpToken);
		tokenProvider.removeToken(VerificationPhoneTokenDto.TOKENTYPE, verificationPhoneToken);
		removeDeletedUser(signUpTokenDto.getEmail()); // 지난 탈퇴 유저 데이터 삭제

		return signUpRequestDto.name();
	}

	/**
	 * 탈퇴 계정을 OAUTH로 계정 복원
	 * 
	 * @param signUpRequestDto
	 * @param accessSignUpOAuthToken
	 * @param verificationPhoneToken
	 * @param clientIp
	 * @return
	 */
	@Transactional
	public String signUpRestorationOAuthUser(SignUpRequestDto signUpRequestDto, String accessSignUpOAuthToken,
			String verificationPhoneToken, String clientIp) {

		CustomOAuth2User customOAuth2User = tokenProvider.getDecryptedTokenDto(CustomOAuth2User.TOKENTYPE,
				accessSignUpOAuthToken);

		// 휴대폰 인증 검증
		verifyPhoneAndCompare(signUpRequestDto.phone(), verificationPhoneToken);

		// 휴대폰 중복 검증
		userService.isPhoneExists(signUpRequestDto.phone());

		// User 수정, Address 등록
		User user = userService.getUserByEmail(customOAuth2User.getEmail() + "_deleted");
		updateUserWithNewDetails(user, signUpRequestDto, UserProvider.OAUTH, null);

		signLogService.signUser(user, SignResult.RESTORATION, clientIp);

		// OAuth 등록
		saveOAuth(user, customOAuth2User, clientIp);

		tokenProvider.removeToken(CustomOAuth2User.TOKENTYPE, accessSignUpOAuthToken);
		tokenProvider.removeToken(VerificationPhoneTokenDto.TOKENTYPE, verificationPhoneToken);
		removeDeletedUser(customOAuth2User.getEmail()); // 지난 탈퇴 유저 데이터 삭제

		return signUpRequestDto.name();
	}

	/**
	 * 소셜 연동
	 * 
	 * @param userId
	 * @param linkOAuthToken
	 */
	public void signUpLinkOAuth(int userId, CustomOAuth2User customOAuth2User, String clientIp) {
		Optional<OAuth> optionalOAuth = oauthService.getOAuth(customOAuth2User.getOauthProvider(),
				customOAuth2User.getOauthProviderId());
		if (optionalOAuth.isPresent())
			throw new DuplicationException(EMAIL_ALREADY_EXISTS);

		User user = userService.getActiveUserByUserId(userId);

		saveOAuth(user, customOAuth2User, clientIp);
	}

	/**
	 * LOCAL 로그인 및 소셜 연동
	 * 
	 * @param signInRequestDto
	 * @param linkOAuthToken
	 * @param clientIp
	 */
	public Map<String, String> signUpLinkOAuth(SignInRequestDto signInRequestDto, String linkOAuthToken,
			String clientIp) {

		Map<String, String> jwts = signInLocalAndCreateJwt(signInRequestDto, clientIp);

		User user = userService.getActiveUserByEmail(signInRequestDto.email());
		CustomOAuth2User customOAuth2User = tokenProvider.getDecryptedTokenDto(TokenType.ACCESS_SIGNUP_OAUTH,
				linkOAuthToken);

		saveOAuth(user, customOAuth2User, clientIp);

		return jwts;
	}

	/**
	 * LOCAL 유저 로그인
	 * 
	 * @param loginRequestDto
	 * @param ip
	 * @return 로그인 토큰들
	 * 
	 * @throws StatusStayException       {@link UserService#getUserByUserProviderAndEmail}
	 * @throws StatusStopException       {@link UserService#getUserByUserProviderAndEmail}
	 * @throws StatusDeleteExceptions    {@link UserService#getUserByUserProviderAndEmail}
	 * 
	 * @throws TooManySignFailException  {@link #validatePassword} 비밀번호 실패 횟수 초과
	 * @throws MismatchPasswordException {@link #validatePassword} 비밀번호 불일치
	 * 
	 * @throws EncryptException          {@link #handleJwt} 암호화 실패
	 * @throws CreateJwtException        {@link #handleJwt} 토큰 생성 실패
	 * @throws RedisSetException         {@link #handleJwt} 리프레쉬 토큰 Redis 저장 실패
	 * 
	 * @throws NoSuchDataException       {@link #signInLocal} 일치하는 유저 없음
	 */
	public Map<String, String> signInLocalAndCreateJwt(SignInRequestDto signInRequestDto, String clientIp) {

		String email = signInRequestDto.email();
		String password = signInRequestDto.password();

		try {
			User user = userService.getActiveUserByEmail(email);

			userService.validatePassword(user, password, clientIp);

			signLogService.signUserAndFailLogResolve(user, SignResult.SIGNIN, clientIp);

			return handleJwt(user);
		} catch (NoSuchDataException e) {
			// 로그인 실패 사유 숨기기
			throw new NoSuchDataException(SIGNIN_FAILED, "일치하는 계정 없음", e);
		}
	}

	public Map<String, String> signInOAuthAndCreateJwt(int userId, String clientIp) {

		User user = userService.getActiveUserByUserId(userId);

		signLogService.signUser(user, SignResult.SIGNIN, clientIp);

		return handleJwt(user);
	}

	/**
	 * 휴대폰 인증 토큰 통해 auth 로그인
	 * 
	 * @param verificationPhoneToken
	 * @param ip
	 * 
	 * @throws IllegalTokenException        {@link TokenProvider#getDecryptedTokenDto}토큰
	 *                                      문자열 null 또는 비어있음
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
	 * @throws EncryptException             {@link JwtProvider#createAuthToken} 암호화
	 *                                      실패
	 * @throws CreateJwtException           {@link JwtProvider#createAuthToken} 토큰
	 *                                      생성 실패
	 * 
	 * @return auth 로그인 토큰
	 */
	public String signInAuthBySms(String verificationPhoneToken, String clientIp) {

		VerificationPhoneTokenDto verificationPhoneTokenDto = tokenProvider
				.getDecryptedTokenDto(VerificationPhoneTokenDto.TOKENTYPE, verificationPhoneToken);
		verificationService.isVerify(verificationPhoneTokenDto.getIntVerificationId());

		String authTokenDto = jwtProvider.createAuthToken(verificationPhoneTokenDto.getVerificationId(),
				verificationPhoneTokenDto.getPhone());

		signLogService.registerSignLog(SignLog.builder().provider("AUTHBYPHONE")
				.id(verificationPhoneTokenDto.getVerificationId()).phone(verificationPhoneTokenDto.getPhone())
				.signResult(SignResult.SIGNIN).clientIp(clientIp).build());

		tokenProvider.removeToken(TokenType.VERIFICATION_PHONE, verificationPhoneToken);

		return authTokenDto;
	}

	/**
	 * 이메일 인증 토큰 통해 auth jwt 발행
	 * 
	 * @param verificationEmailToken
	 * @param ip
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
	 * @throws EncryptException             {@link JwtProvider#createAuthToken} 암호화
	 *                                      실패
	 * @throws CreateJwtException           {@link JwtProvider#createAuthToken} 토큰
	 *                                      생성 실패
	 * 
	 * @return auth jwt
	 */
	public String signInAuthByEmail(String verificationEmailToken, String clientIp) {

		VerificationEmailTokenDto verificationEmailTokenDto = tokenProvider
				.getDecryptedTokenDto(VerificationEmailTokenDto.TOKENTYPE, verificationEmailToken);
		verificationService.isVerify(verificationEmailTokenDto.getIntVerificationId());

		String authTokenDto = jwtProvider.createAuthToken(verificationEmailTokenDto.getVerificationId(),
				verificationEmailTokenDto.getEmail());

		signLogService.registerSignLog(SignLog.builder().provider("AUTHBYEMAIL")
				.id(verificationEmailTokenDto.getVerificationId()).email(verificationEmailTokenDto.getEmail())
				.signResult(SignResult.SIGNIN).clientIp(clientIp).build());

		tokenProvider.removeToken(TokenType.VERIFICATION_EMAIL, verificationEmailToken);

		return authTokenDto;
	}

	/**
	 * accessToken의 기한 만료로 토큰 재발급
	 * 
	 * @param accessToken
	 * @param refreshToken
	 * @param clientIp
	 * 
	 * @throws TimeOutJwtException         {@link JwtProvider#validateRefreshToken}
	 *                                     시간 초과
	 * @throws DecryptException            {@link JwtProvider#validateRefreshToken}
	 *                                     복호화 실패
	 * @throws ValidateJwtException        {@link JwtProvider#validateRefreshToken}
	 *                                     JWT 파싱 실패
	 * 
	 * @throws NoSuchTokenException        {@link TokenProvider#validRefreshToken}
	 *                                     Redis에 없는 키
	 * @throws TokenValueMismatchException {@link TokenProvider#validRefreshToken}
	 *                                     Redis 값과 비교 불일치
	 * 
	 * @throws NoSuchDataException         {@link UserService#getUserByUserId} 찾을 수
	 *                                     없는 유저
	 * @throws StatusStayException         {@link UserService#getUserByUserId} 일시정지
	 *                                     유저
	 * @throws StatusStopException         {@link UserService#getUserByUserId} 중지 유저
	 * @throws StatusDeleteExceptions      {@link UserService#getUserByUserId} 탈퇴 유저
	 * 
	 * @throws EncryptException            {@link #handleJwt} 암호화 실패
	 * @throws CreateJwtException          {@link #handleJwt} 토큰 생성 실패
	 * @throws RedisSetException           {@link #handleJwt} 리프레쉬 토큰 Redis 저장 실패
	 * 
	 * @return 로그인 토큰들
	 */
	@Transactional
	public Map<String, String> refresh(String refreshToken, String clientIp) {
		try {
//			리프레쉬 토큰 검증
			String userIdStr = jwtProvider.validateRefreshToken(refreshToken);
			tokenProvider.validRefreshToken(userIdStr, refreshToken);

			int userId = Integer.parseInt(userIdStr);
			User user = userService.getActiveUserByUserId(userId);

			Map<String, String> signJwts = handleJwt(user);

			signLogService.signUser(user, SignResult.REFRESH, clientIp);

			return signJwts;
		} catch (Exception e) {
			signLogService.registerSignLog(SignLog.builder().clientIp(clientIp).signResult(SignResult.REFRESH_FAIL)
					.reason(e.getMessage()).build());
			throw e;
		}
	}

	@Transactional
	public void convertToLocal(int userId, String verificationEmailToken, String email, String rawPassword,
			String clientIp) {
		VerificationEmailTokenDto verificationEmailTokenDto = tokenProvider
				.getDecryptedTokenDto(TokenType.VERIFICATION_EMAIL, verificationEmailToken);
		verificationEmailTokenDto.compareEmail(email);

		User user = userService.getActiveUserByUserId(userId);
		UserProvider previousUserProvider = user.getUserProvider();
		if (Objects.equals(previousUserProvider, UserProvider.LOCAL)) {
			throw new ForbiddenException(AUTHORIZATION_FAILED, "이미 통합 유저.");
		}
		String encodedPassword = passwordEncoder.encode(rawPassword);
		user.setPassword(encodedPassword);
		user.setUserProvider(UserProvider.LOCAL);

		signLogService.registerSignLog(SignLog.builder().provider(previousUserProvider.name()).id(userId + "")
				.clientIp(clientIp).signResult(SignResult.CONVERTTOLOCAL).build());
	}

	/**
	 * LOCAL, OAUTH 로그아웃
	 * 
	 * @param accessToken
	 * @param refreshToken
	 * @param clientIp
	 * 
	 * @throws TimeOutJwtException       {@link JwtProvider#getSignJwtRemainingTime},
	 *                                   {@link JwtProvider#validateRefreshToken} 시간
	 *                                   초과
	 * @throws ValidateJwtException      {@link JwtProvider#getSignJwtRemainingTime},
	 *                                   {@link JwtProvider#validateRefreshToken}
	 *                                   JWT 파싱 실패
	 * 
	 * @throws RedisSetException         {@link TokenProvider#addAccessJwtBlacklist}
	 *                                   Redis 저장 중 오류
	 * 
	 * @throws DecryptException          {@link JwtProvider#validateRefreshToken}
	 *                                   복호화 실패
	 * 
	 * @throws IllegalJwtException(@link #signOutLocal)
	 */
	public void signOut(String provider, String userId, String accessToken, String refreshToken, String clientIp) {
		log.debug("* [{}] 로그아웃 요청.", provider);
		log.debug("* AccessToken: [{}]", LogMaskingUtil.maskToken(accessToken, MaskLevel.MEDIUM));
		log.debug("* RefreshToken: [{}]", LogMaskingUtil.maskToken(refreshToken, MaskLevel.MEDIUM));
		signLogService.registerSignLog(SignLog.builder().provider(provider).id(userId).clientIp(clientIp)
				.signResult(SignResult.SIGNOUT).build());
		
//		1. accessToken 블랙리스트 등록
		try {
			long ttl = jwtProvider.getAccessTokenRemainingTime(accessToken);
			tokenProvider.addTokenBlacklist(accessToken, ttl, "SIGNOUT");
		} catch (TimeOutJwtException e) {
			log.debug("* 이미 만료된 AT.");
			return;
		}

//		2. refreshToken 삭제
		String userIdStr = jwtProvider.validateRefreshToken(refreshToken);
		if (!Objects.equals(userIdStr, userId)) {
			log.warn(
					"* 로그아웃 중 RefreshToken 부적합. 도용된 userId: [{}], RefreshToken.userId: [{}], RefreshToken: [{}], RequestIp: [{}]",
					userId, userIdStr, refreshToken, clientIp);
			return;
		}
		if (!(tokenProvider.removeToken(TokenType.JWT_REFRESH, userIdStr))) {
			log.warn("* 로그아웃 중 RefreshToken 삭제 실패. 도용된 RefreshToken.userId: [{}], RefreshToken: [{}], RequestIp: [{}]",
					userIdStr, refreshToken, clientIp);
			return;
		}


	}

	/**
	 * AUTH 로그아웃
	 * 
	 * @param accessToken
	 * @param refreshToken
	 * @param clientIp
	 * 
	 * @throws TimeOutJwtException       {@link JwtProvider#getSignJwtRemainingTime},
	 *                                   {@link JwtProvider#validateRefreshToken} 시간
	 *                                   초과
	 * @throws ValidateJwtException      {@link JwtProvider#getSignJwtRemainingTime},
	 *                                   {@link JwtProvider#validateRefreshToken}
	 *                                   JWT 파싱 실패
	 * 
	 * @throws RedisSetException         {@link TokenProvider#addAccessJwtBlacklist}
	 *                                   Redis 저장 중 오류
	 * 
	 * @throws DecryptException          {@link JwtProvider#validateRefreshToken}
	 *                                   복호화 실패
	 * 
	 * @throws IllegalJwtException(@link #signOutLocal)
	 */
	@Transactional
	public void signOutAuth(String authToken, String clientIp) {
		log.debug("* AUTH 로그아웃 요청.");

//		authToken 블랙리스트 등록
		try {
			long ttl = jwtProvider.getAuthTokenRemainingTime(authToken);
			tokenProvider.addTokenBlacklist(authToken, ttl, "SIGNOUT");
		} catch (TimeOutJwtException e) {
			log.debug("* 이미 만료된 AuthToken.");
		}
	}

	/**
	 * OAUTH 연동 해제
	 * 
	 * @param userId
	 * @param unlinkOAuthRequestDto
	 * @param clientIp
	 */
	public void unlinkOAuth(int userId, UnlinkOAuthRequestDto unlinkOAuthRequestDto, String clientIp) {
		User user = userService.getActiveUserByUserId(userId);
		if (!Objects.equals(user.getUserProvider(), UserProvider.LOCAL)) {
			throw new ForbiddenException(AUTHORIZATION_FAILED, "통합 유저가 아님.");
		}
		OAuth oauth = oauthService.getOAuthByOAuthId(unlinkOAuthRequestDto.getOAuthId(obfuscator));
		if (!Objects.equals(oauth.getUser().getUserId(), userId)) {
			throw new ForbiddenException(AUTHORIZATION_FAILED, "연동 유저가 일치하지 않음.");
		}

		oauthService.deleteOAuth(oauth.getOauthId(), clientIp);
	}

	/**
	 * 회원탈퇴 (유저 상태 DELETE로 변경)
	 * 
	 * @param userProvider
	 * @param email
	 * @param passwordToken
	 * 
	 * @throws IllegalTokenException       {@link TokenProvider#getDecryptedTokenDto}
	 *                                     토큰 문자열 null 또는 비어있음
	 * @throws NoSuchTokenException        {@link TokenProvider#getDecryptedTokenDto}
	 *                                     Redis 일치하는 토큰 없음
	 * @throws DecryptException            {@link TokenProvider#getDecryptedTokenDto}
	 *                                     복호화 실패
	 * @throws RedisGetException           {@link TokenProvider#getDecryptedTokenDto}
	 *                                     Redis 조회 실패
	 * 
	 * @throws TokenValueMismatchException {@link #deleteUser} 로그인되어있는 userId와
	 *                                     PasswordToken.userId 불일치
	 * 
	 * @throws NoSuchDataException         {@link #getUserByUserId} 찾을 수 없는 유저
	 * @throws StatusStayException         {@link #getUserByUserId} 일시정지 유저
	 * @throws StatusStopException         {@link #getUserByUserId} 중지 유저
	 * @throws StatusDeleteException       {@link #getUserByUserId} 탈퇴 유저
	 */
	@Transactional
	public void deleteUser(int userId, String clientIp) {
		log.debug("* Soft Delete Local 회원 탈퇴. UserId: [{}]", LogMaskingUtil.maskId(userId, MaskLevel.MEDIUM));

		User user = userService.getActiveUserByUserId(userId);
		String anonymizedEmail = user.getEmail() + "_deleted";
		user.setEmail(anonymizedEmail);
		user.setUserStatus(UserStatus.DELETE);
		user.setName("탈퇴한사용자");
		userRepository.save(user);

		oauthService.deleteOAuthByUserId(userId, clientIp);

		signLogService.registerSignLog(SignLog.builder().provider(user.getUserProvider().name()).id(userId + "")
				.email(user.getEmail()).clientIp(clientIp).signResult(SignResult.DELETE).build());
	}

	/**
	 * 탈퇴 유저 데이터 삭제
	 * 
	 * @param email
	 */
	@Transactional
	private void removeDeletedUser(String email) {
		String deletedEmail = email + "_deleted";
		Optional<User> optionalDeleteduser = userRepository.findByEmailAndUserStatus(deletedEmail, UserStatus.DELETE);
		if (optionalDeleteduser.isPresent()) {
			User deletedUser = optionalDeleteduser.get();
			deletedUser.setEmail(deletedUser.getUserId()+"_deleted");
			deletedUser.setPassword(null);
			deletedUser.setBirth(null);
			deletedUser.setPhone(null);
			deletedUser.setMarketingReceivedStatus(false);

			addressRepository.deleteByUserId(deletedUser.getUserId());
		}
	}

	private void saveOAuth(User user, CustomOAuth2User customOAuth2User, String clientIp) {
		OAuth oauth = customOAuth2User.toEntity();
		oauth.setOauthStatus(OAuthStatus.ACTIVE);
		oauth.setUser(user);
		oauthRepository.save(oauth);

		signLogService
				.registerSignLog(SignLog.builder().provider(oauth.getOauthProvider().name()).id(oauth.getOauthId() + "")
						.email(customOAuth2User.getEmail()).signResult(SignResult.SIGNUP).clientIp(clientIp).build());
	}

	/**
	 * User 정보 통해 로그인 토큰들 발급 및 RefreshToken은 Redis 저장
	 * 
	 * @param user
	 * 
	 * @throws EncryptException   {@link JwtProvider#createSignToken} 암호화 실패
	 * @throws CreateJwtException {@link JwtProvider#createSignToken} 토큰 생성 실패
	 * 
	 * @throws RedisSetException  {@link TokenProvider#addRefreshJwt} 리프레쉬 토큰 Redis
	 *                            저장 실패
	 * @return 로그인 토큰들
	 */
	private Map<String, String> handleJwt(User user) {
		Map<String, String> signToken = jwtProvider.createSignToken(user.getUserId(),
				List.of(roles.get(user.getUserProvider())),
				Map.of("provider", user.getUserProvider().name(), "email", user.getEmail(), "name", user.getName()));

		tokenProvider.addRefreshJwt(user.getUserId(), signToken.get("refreshToken"));
		return signToken;
	}

	private User updateUserWithNewDetails(User userToRestore, SignUpRequestDto newDetails, UserProvider provider,
			String newPassword) {
		String deltedEmail = userToRestore.getEmail();
		userToRestore.setEmail(deltedEmail.replace("_deleted", ""));
		userToRestore.setPassword(newPassword);
		userToRestore.setName(newDetails.name());
		userToRestore.setBirth(newDetails.birth());
		userToRestore.setPhone(newDetails.phone());
		userToRestore.setUserProvider(provider);
		userToRestore.setUserStatus(UserStatus.ACTIVE);
		userToRestore.setMarketingReceivedStatus(newDetails.marketingReceivedStatus());
		userRepository.save(userToRestore);

		// 주소 생성 로직
		Address address = newDetails.toAddressEntity(userToRestore);
		addressRepository.unsetPrimaryForUser(userToRestore.getUserId());
		addressRepository.save(address);

		return userToRestore;
	}

	private void verifyPhoneAndCompare(String requestPhone, String verificationPhoneToken) {
		VerificationPhoneTokenDto verificationTokenDto = tokenProvider
				.getDecryptedTokenDto(VerificationPhoneTokenDto.TOKENTYPE, verificationPhoneToken);
		verificationService.isVerify(verificationTokenDto.getIntVerificationId());
		if (!Objects.equals(requestPhone, verificationTokenDto.getPhone())) {
			throw new IllegalRequestException(ILLEGAL_INPUT_VALUE, "* 입력된 휴대폰 번호와 인증된 번호가 다릅니다.");
		}
	}

}
