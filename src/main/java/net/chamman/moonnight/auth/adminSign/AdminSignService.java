package net.chamman.moonnight.auth.adminSign;

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
import net.chamman.moonnight.auth.adminSign.dto.AdminSignInRequestDto;
import net.chamman.moonnight.auth.adminSign.dto.AdminSignUpRequestDto;
import net.chamman.moonnight.auth.adminSign.log.AdminSignLog;
import net.chamman.moonnight.auth.adminSign.log.AdminSignLog.SignResult;
import net.chamman.moonnight.auth.adminSign.log.AdminSignLogService;
import net.chamman.moonnight.auth.token.JwtProvider;
import net.chamman.moonnight.auth.token.TokenProvider;
import net.chamman.moonnight.auth.token.TokenProvider.TokenType;
import net.chamman.moonnight.auth.token.dto.AdminSignUpTokenDto;
import net.chamman.moonnight.auth.token.dto.VerificationEmailTokenDto;
import net.chamman.moonnight.auth.token.dto.VerificationPhoneTokenDto;
import net.chamman.moonnight.auth.verification.VerificationService;
import net.chamman.moonnight.domain.admin.Admin;
import net.chamman.moonnight.domain.admin.Admin.AdminStatus;
import net.chamman.moonnight.domain.admin.AdminRepository;
import net.chamman.moonnight.domain.admin.AdminService;
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
public class AdminSignService {

	private final AdminRepository adminRepository;
	private final AdminService adminService;
	private final AdminSignLogService adminSignLogService;
	private final VerificationService verificationService;

	private final JwtProvider jwtProvider;
	private final TokenProvider tokenProvider;
	private final PasswordEncoder passwordEncoder;

	/**
	 * 관리자 회원가입 1차
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
	 * @throws DuplicationException         {@link AdminService#isEmailExists} 이메일 중복
	 * 
	 * @throws EncryptException             {@link TokenProvider#createToken} 암호화 실패
	 * @throws RedisSetException            {@link TokenProvider#createToken} Redis
	 *                                      저장 실패
	 * 
	 * @return
	 */
	@Transactional
	public String signUpToken(String email, String password, String valificationEmailToken) {
		log.debug("* 관리자 회원 가입 1차 요청. email: [{}] with verificationEmailToken: [{}]",
				LogMaskingUtil.maskEmail(email, MaskLevel.MEDIUM),
				LogMaskingUtil.maskToken(valificationEmailToken, MaskLevel.MEDIUM));

		VerificationEmailTokenDto verificationEmailTokenDto = tokenProvider
				.getDecryptedTokenDto(VerificationEmailTokenDto.TOKENTYPE, valificationEmailToken);
		verificationService.isVerify(verificationEmailTokenDto.getIntVerificationId());
		if (!Objects.equals(verificationEmailTokenDto.getEmail(), email)) {
			throw new IllegalRequestException(ILLEGAL_INPUT_VALUE);
		}

		// 이메일 중복 검사
		adminService.isEmailExistsForRegistration(email);

		String token = tokenProvider.createToken(new AdminSignUpTokenDto(email, password), AdminSignUpTokenDto.TOKENTYPE);

		tokenProvider.removeToken(TokenType.VERIFICATION_EMAIL, valificationEmailToken);

		return token;
	}

	/**
	 * 관리자 회원가입 2차
	 * 
	 * @param adminCreateRequestDto
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
	 * @throws DuplicationException         {@link AdminService#isPhoneExists} 휴대폰 중복
	 * 
	 * @return 회원 가입 유저 이름
	 */
	@Transactional
	public String signUp(AdminSignUpRequestDto signUpRequestDto, String accessSignUpToken,
			String verificationPhoneToken, String clientIp) {

		AdminSignUpTokenDto signUpTokenDto = tokenProvider.getDecryptedTokenDto(AdminSignUpTokenDto.TOKENTYPE, accessSignUpToken);

		VerificationPhoneTokenDto verificationPhoneTokenDto = tokenProvider
				.getDecryptedTokenDto(VerificationPhoneTokenDto.TOKENTYPE, verificationPhoneToken);
		verificationService.isVerify(verificationPhoneTokenDto.getIntVerificationId());
		if (!Objects.equals(signUpRequestDto.phone(), verificationPhoneTokenDto.getPhone())) {
			log.debug("* 관리자 회원 가입 중 휴대폰 번호 입력 값 상이.");
			throw new IllegalRequestException(ILLEGAL_INPUT_VALUE);
		}

		adminService.isPhoneExists(verificationPhoneTokenDto.getPhone());

		// 비밀번호 인코딩 후 저장
		String encodedPassoword = passwordEncoder.encode(signUpTokenDto.getRawPassword());

		Admin admin = signUpRequestDto.toEntity();
		admin.setEmail(signUpTokenDto.getEmail());
		admin.setPassword(encodedPassoword);
		admin.setPhone(verificationPhoneTokenDto.getPhone());
		adminRepository.save(admin);

		adminSignLogService.signAdmin(admin, SignResult.SIGNUP, clientIp);

		tokenProvider.removeToken(AdminSignUpTokenDto.TOKENTYPE, accessSignUpToken);
		tokenProvider.removeToken(VerificationPhoneTokenDto.TOKENTYPE, verificationPhoneToken);
		removeDeletedAdmin(signUpTokenDto.getEmail()); // 지난 탈퇴 유저 데이터 삭제

		return signUpRequestDto.name();
	}

	/**
	 * 관리자 로그인
	 * 
	 * @param loginRequestDto
	 * @param ip
	 * @return 로그인 토큰들
	 * 
	 * @throws StatusStayException       {@link AdminService#getAdminByAdminProviderAndEmail}
	 * @throws StatusStopException       {@link AdminService#getAdminByAdminProviderAndEmail}
	 * @throws StatusDeleteExceptions    {@link AdminService#getAdminByAdminProviderAndEmail}
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
	public Map<String, String> signInAndCreateJwt(AdminSignInRequestDto signInRequestDto, String clientIp) {

		String email = signInRequestDto.email();
		String password = signInRequestDto.password();

		try {
			Admin admin = adminService.getActiveAdminByEmail(email);

			adminService.validatePassword(admin, password, clientIp);

			adminSignLogService.signAdminAndFailLogResolve(admin, SignResult.SIGNIN, clientIp);

			return handleJwt(admin);
		} catch (NoSuchDataException e) {
			// 로그인 실패 사유 숨기기
			throw new NoSuchDataException(SIGNIN_FAILED, "일치하는 계정 없음", e);
		}
	}

	/**
		 * 관리자 로그아웃
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
		public void signOut(int adminId, String accessToken, String refreshToken, String clientIp) {
			log.debug("* AccessToken: [{}]", LogMaskingUtil.maskToken(accessToken, MaskLevel.MEDIUM));
			log.debug("* RefreshToken: [{}]", LogMaskingUtil.maskToken(refreshToken, MaskLevel.MEDIUM));
			Admin admin = adminService.getActiveAdminByAdminId(adminId);
			adminSignLogService.registerSignLog(AdminSignLog.builder().admin(admin).clientIp(clientIp)
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
			String adminIdStr = jwtProvider.validateRefreshToken(refreshToken);
			if (!Objects.equals(adminIdStr, adminId)) {
				log.warn(
						"* 로그아웃 중 RefreshToken 부적합. 도용된 adminId: [{}], RefreshToken.adminId: [{}], RefreshToken: [{}], RequestIp: [{}]",
						adminId, adminIdStr, refreshToken, clientIp);
				return;
			}
			if (!(tokenProvider.removeToken(TokenType.JWT_REFRESH, adminIdStr))) {
				log.warn("* 로그아웃 중 RefreshToken 삭제 실패. 도용된 RefreshToken.adminId: [{}], RefreshToken: [{}], RequestIp: [{}]",
						adminIdStr, refreshToken, clientIp);
				return;
			}
	
	
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
	 * @throws NoSuchDataException         {@link AdminService#getAdminByAdminId} 찾을 수
	 *                                     없는 유저
	 * @throws StatusStayException         {@link AdminService#getAdminByAdminId} 일시정지
	 *                                     유저
	 * @throws StatusStopException         {@link AdminService#getAdminByAdminId} 중지 유저
	 * @throws StatusDeleteExceptions      {@link AdminService#getAdminByAdminId} 탈퇴 유저
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
			String adminIdStr = jwtProvider.validateRefreshToken(refreshToken);
			tokenProvider.validRefreshToken(adminIdStr, refreshToken);

			int adminId = Integer.parseInt(adminIdStr);
			Admin admin = adminService.getActiveAdminByAdminId(adminId);

			Map<String, String> signJwts = handleJwt(admin);

			adminSignLogService.signAdmin(admin, SignResult.REFRESH, clientIp);

			return signJwts;
		} catch (Exception e) {
			adminSignLogService.registerSignLog(AdminSignLog.builder().clientIp(clientIp).signResult(SignResult.REFRESH_FAIL)
					.reason(e.getMessage()).build());
			throw e;
		}
	}

	/**
	 * 회원탈퇴 (유저 상태 DELETE로 변경)
	 * 
	 * @param adminProvider
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
	 * @throws TokenValueMismatchException {@link #deleteAdmin} 로그인되어있는 adminId와
	 *                                     PasswordToken.adminId 불일치
	 * 
	 * @throws NoSuchDataException         {@link #getAdminByAdminId} 찾을 수 없는 유저
	 * @throws StatusStayException         {@link #getAdminByAdminId} 일시정지 유저
	 * @throws StatusStopException         {@link #getAdminByAdminId} 중지 유저
	 * @throws StatusDeleteException       {@link #getAdminByAdminId} 탈퇴 유저
	 */
	@Transactional
	public void deleteAdmin(int adminId, String clientIp) {
		log.debug("* Soft Delete Local 회원 탈퇴. AdminId: [{}]", LogMaskingUtil.maskId(adminId, MaskLevel.MEDIUM));

		Admin admin = adminService.getActiveAdminByAdminId(adminId);
		String anonymizedEmail = admin.getEmail() + "_deleted";
		admin.setEmail(anonymizedEmail);
		admin.setAdminStatus(AdminStatus.DELETE);
		admin.setName("탈퇴한사용자");
		adminRepository.save(admin);

		adminSignLogService.registerSignLog(AdminSignLog.builder().admin(admin).clientIp(clientIp).signResult(SignResult.DELETE).build());
	}

	/**
	 * 탈퇴 유저 데이터 삭제
	 * 
	 * @param email
	 */
	@Transactional
	private void removeDeletedAdmin(String email) {
		String deletedEmail = email + "_deleted";
		Optional<Admin> optionalDeletedadmin = adminRepository.findByEmailAndAdminStatus(deletedEmail, AdminStatus.DELETE);
		if (optionalDeletedadmin.isPresent()) {
			Admin deletedAdmin = optionalDeletedadmin.get();
			deletedAdmin.setEmail(deletedAdmin.getAdminId()+"_deleted");
			deletedAdmin.setPassword(null);
			deletedAdmin.setPhone(null);

		}
	}
	
	/**
	 * Admin 정보 통해 로그인 토큰들 발급 및 RefreshToken은 Redis 저장
	 * 
	 * @param admin
	 * 
	 * @throws EncryptException   {@link JwtProvider#createSignToken} 암호화 실패
	 * @throws CreateJwtException {@link JwtProvider#createSignToken} 토큰 생성 실패
	 * 
	 * @throws RedisSetException  {@link TokenProvider#addRefreshJwt} 리프레쉬 토큰 Redis
	 *                            저장 실패
	 * @return 로그인 토큰들
	 */
	private Map<String, String> handleJwt(Admin admin) {
		Map<String, String> signToken = jwtProvider.createSignToken(admin.getAdminId(),
				List.of("ROLE_ADMIN"), Map.of("email", admin.getEmail(), "name", admin.getName()));

		tokenProvider.addRefreshJwt(admin.getAdminId(), signToken.get("refreshToken"));
		return signToken;
	}


}
