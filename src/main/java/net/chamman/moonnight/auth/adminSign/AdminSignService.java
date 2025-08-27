package net.chamman.moonnight.auth.adminSign;

import static net.chamman.moonnight.global.exception.HttpStatusCode.EMAIL_ALREADY_EXISTS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.ILLEGAL_INPUT_VALUE;
import static net.chamman.moonnight.global.exception.HttpStatusCode.PHONE_ALREADY_EXISTS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.SIGNIN_FAILED;
import static net.chamman.moonnight.global.exception.HttpStatusCode.USER_NOT_FOUND;
import static net.chamman.moonnight.global.exception.HttpStatusCode.USER_STATUS_DELETE;
import static net.chamman.moonnight.global.exception.HttpStatusCode.USER_STATUS_STOP;

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
import net.chamman.moonnight.auth.token.dto.FindAdminPwTokenDto;
import net.chamman.moonnight.auth.token.dto.VerificationEmailTokenDto;
import net.chamman.moonnight.auth.token.dto.VerificationPhoneTokenDto;
import net.chamman.moonnight.auth.verification.VerificationService;
import net.chamman.moonnight.domain.admin.Admin;
import net.chamman.moonnight.domain.admin.Admin.AdminStatus;
import net.chamman.moonnight.domain.admin.AdminRepository;
import net.chamman.moonnight.domain.admin.dto.AdminResponseDto;
import net.chamman.moonnight.global.exception.IllegalRequestException;
import net.chamman.moonnight.global.exception.MismatchPasswordException;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.global.exception.VersionMismatchException;
import net.chamman.moonnight.global.exception.admin.DuplicationException;
import net.chamman.moonnight.global.exception.jwt.TimeOutJwtException;
import net.chamman.moonnight.global.exception.redis.RedisGetException;
import net.chamman.moonnight.global.exception.sign.TooManySignFailException;
import net.chamman.moonnight.global.exception.status.StatusDeleteException;
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
public class AdminSignService {

	private final AdminRepository adminRepository;
	private final AdminSignLogService adminSignLogService;
	private final VerificationService verificationService;
	private final JwtProvider jwtProvider;
	private final TokenProvider tokenProvider;
	private final PasswordEncoder passwordEncoder;

	/**
	 * 관리자 로그인
	 * 
	 * @param signInRequestDto
	 * @param clientIp
	 * 
	 * @return
	 */
	public Map<String, String> signInAndCreateJwt(AdminSignInRequestDto signInRequestDto, String clientIp) {

		String email = signInRequestDto.email();
		String password = signInRequestDto.password();

		try {
			Admin admin = findAdminWithValidateStatusByEmail(email);

			validatePassword(admin, password, clientIp);

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
	 * @param adminId
	 * @param accessToken
	 * @param refreshToken
	 * @param clientIp
	 */
	public void signOut(int adminId, String accessToken, String refreshToken, String clientIp) {
		log.debug("* AccessToken: [{}]", LogMaskingUtil.maskToken(accessToken, MaskLevel.MEDIUM));
		log.debug("* RefreshToken: [{}]", LogMaskingUtil.maskToken(refreshToken, MaskLevel.MEDIUM));
		Admin admin = findAdminWithValidateStatusByAdminId(adminId);
		adminSignLogService.registerSignLog(
				AdminSignLog.builder().admin(admin).clientIp(clientIp).signResult(SignResult.SIGNOUT).build());

		// 1. accessToken 블랙리스트 등록
		try {
			long ttl = jwtProvider.getAccessTokenRemainingTime(accessToken);
			tokenProvider.addTokenBlacklist(accessToken, ttl, "SIGNOUT");
		} catch (TimeOutJwtException e) {
			log.debug("* 이미 만료된 AT.");
			return;
		}

		// 2. refreshToken 삭제
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
	 * 이메일로 관리자를 조회하여 중복 검사.
	 * <p>
	 * 회원 가입 1차 프로세스의 일부로 사용되며, 이메일 주소를 기반으로 **탈퇴(DELETE) 상태가 아닌** 관리자 계정을 찾아 중복 검사 합니다.
	 *
	 * @param email 중복 검사할 관리자의 이메일 주소
	 * 
	 * @throws DuplicationException 해당 이메일로 가입된 계정이 있는 경우
	 * 
	 * @apiNote 이 메서드 호출 시 발생할 수 있는 예외와 그에 따른 HTTP 응답 코드입니다.
	 *          <li>Admin Account Errors</li>
	 *          <li>{@code DuplicationException}: 409 Conflict (Code: 4531)</li>
	 *          </ul>
	 */
	public void isEmailExistsForSignUp(String email) {
		log.debug("* 이메일로 관리자를 조회하여 중복 검사. email: {}", LogMaskingUtil.maskEmail(email, MaskLevel.MEDIUM));

		Optional<Admin> existAdmin = adminRepository.findByEmailAndAdminStatusNot(email, AdminStatus.DELETE);
		if (existAdmin.isPresent()) {
			throw new DuplicationException(EMAIL_ALREADY_EXISTS);
		}
	}

	/**
	 * 이메일, 이메일인증토큰, 비밀번호 통해 관리자 회원가입 1차 토큰 발행
	 * <p>
	 * 회원 가입 1차 프로세스의 일부로 사용되며,
	 * 
	 * @param email
	 * @param password
	 * @param valificationEmailToken
	 * 
	 * @return
	 */
	@Transactional
	public String createSignUpToken(String email, String password, String confirmPassword,
			String valificationEmailToken) {
		log.debug("* 관리자 회원 가입 1차 요청. email: [{}], verificationEmailToken: [{}]",
				LogMaskingUtil.maskEmail(email, MaskLevel.MEDIUM),
				LogMaskingUtil.maskToken(valificationEmailToken, MaskLevel.MEDIUM));

		if (!Objects.equals(password, confirmPassword)) {
			throw new IllegalRequestException(ILLEGAL_INPUT_VALUE,
					"두 비밀번호가 일치하지 않음. password: " + password + ", confirmPassword: " + confirmPassword);
		}

		VerificationEmailTokenDto verificationEmailTokenDto = tokenProvider
				.getDecryptedTokenDto(VerificationEmailTokenDto.TOKENTYPE, valificationEmailToken);
		verificationService.isVerify(verificationEmailTokenDto.getIntVerificationId());
		if (!Objects.equals(verificationEmailTokenDto.getEmail(), email)) {
			throw new IllegalRequestException(ILLEGAL_INPUT_VALUE);
		}

		// 이메일 중복 검사
		isEmailExistsForSignUp(email);

		String token = tokenProvider.createToken(new AdminSignUpTokenDto(email, password),
				AdminSignUpTokenDto.TOKENTYPE);

		tokenProvider.removeToken(TokenType.VERIFICATION_EMAIL, valificationEmailToken);

		return token;
	}

	/**
	 * 휴대폰 번호로 관리자를 조회하여 중복 검사.
	 * <p>
	 * 회원 가입 2차 프로세스의 일부로 사용되며, 휴대폰 번호를 기반으로 **탈퇴(DELETE) 상태가 아닌** 관리자 계정을 찾아 중복 검사 합니다.
	 *
	 * @param phone 중복 검사할 관리자의 휴대폰 번호
	 * 
	 * @throws DuplicationException 해당 휴대폰 번호로 가입된 계정이 있는 경우
	 * 
	 * @apiNote 이 메서드 호출 시 발생할 수 있는 예외와 그에 따른 HTTP 응답 코드입니다.
	 *          <li>Admin Account Errors</li>
	 *          <li>{@code DuplicationException}: 409 Conflict (Code: 4532)</li>
	 *          </ul>
	 */
	public void isPhoneExistForSignUp(String phone) {
		log.debug("* 휴대폰 번호로 관리자를 조회하여 중복 검사. phone: {}", LogMaskingUtil.maskPhone(phone, MaskLevel.MEDIUM));

		Optional<Admin> existAdminProvider = adminRepository.findByPhoneAndAdminStatusNot(phone, AdminStatus.DELETE);
		if (existAdminProvider.isPresent()) {
			throw new DuplicationException(PHONE_ALREADY_EXISTS);
		}
	}

	/**
	 * 관리자 회원가입 1차 토큰, 휴대폰 인증 토큰, 세부정보 데이터통해 관리자 회원가입
	 * <p>
	 * 회원 가입 2차 프로세스의 일부로 사용.
	 * 
	 * @param signUpRequestDto       관리자 세부 정보
	 * @param accessSignUpToken      관리자 회원가입 1차 토큰
	 * @param verificationPhoneToken 휴대폰 인증 토큰
	 * @param clientIp
	 * 
	 * @return
	 */
	@Transactional
	public String signUp(AdminSignUpRequestDto dto, String accessSignUpToken, String verificationPhoneToken,
			String clientIp) {

		AdminSignUpTokenDto signUpTokenDto = tokenProvider.getDecryptedTokenDto(AdminSignUpTokenDto.TOKENTYPE,
				accessSignUpToken);

		VerificationPhoneTokenDto verificationPhoneTokenDto = tokenProvider
				.getDecryptedTokenDto(VerificationPhoneTokenDto.TOKENTYPE, verificationPhoneToken);
		verificationService.isVerify(verificationPhoneTokenDto.getIntVerificationId());
		if (!Objects.equals(dto.phone(), verificationPhoneTokenDto.getPhone())) {
			log.debug("* 관리자 회원 가입 중 휴대폰 번호 입력 값 상이.");
			throw new IllegalRequestException(ILLEGAL_INPUT_VALUE);
		}

		isPhoneExistForSignUp(verificationPhoneTokenDto.getPhone());

		// 비밀번호 인코딩 후 저장
		String encodedPassoword = passwordEncoder.encode(signUpTokenDto.getRawPassword());

		Admin admin = Admin.builder()
				.name(dto.name())
				.phone(verificationPhoneTokenDto.getPhone())
				.adminStatus(AdminStatus.ACTIVE)
				.email(signUpTokenDto.getEmail())
				.password(encodedPassoword)
				.build();
		adminRepository.save(admin);

		adminSignLogService.signAdmin(admin, SignResult.SIGNUP, clientIp);

		tokenProvider.removeToken(AdminSignUpTokenDto.TOKENTYPE, accessSignUpToken);
		tokenProvider.removeToken(VerificationPhoneTokenDto.TOKENTYPE, verificationPhoneToken);

		return dto.name();
	}

	/**
	 * 회원탈퇴 (유저 상태 DELETE로 변경)
	 * 
	 * @param adminId
	 * @param rawPassword
	 * @param accessToken
	 * @param refreshToken
	 * @param clientIp
	 */
	@Transactional
	public void softDeleteAdmin(int adminId, String rawPassword, String accessToken, String refreshToken,
			String clientIp) {
		log.debug("* Soft Delete Local 회원 탈퇴. AdminId: [{}]", LogMaskingUtil.maskId(adminId, MaskLevel.MEDIUM));

		Admin admin = findAdminWithValidateStatusByAdminId(adminId);
		validatePassword(admin, rawPassword, clientIp);
		admin.softDelete();

		adminSignLogService.registerSignLog(
				AdminSignLog.builder().admin(admin).clientIp(clientIp).signResult(SignResult.DELETE).build());

		signOut(adminId, accessToken, refreshToken, clientIp);
	}

	/**
	 * accessToken의 기한 만료로 토큰 재발급
	 * 
	 * @param refreshToken
	 * @param clientIp
	 * 
	 * @return
	 */
	@Transactional
	public Map<String, String> refresh(String refreshToken, String clientIp) {
		try {
//			리프레쉬 토큰 검증
			String adminIdStr = jwtProvider.validateRefreshToken(refreshToken);
			tokenProvider.validRefreshToken(adminIdStr, refreshToken);

			int adminId = Integer.parseInt(adminIdStr);
			Admin admin = findAdminWithValidateStatusByAdminId(adminId);

			Map<String, String> signJwts = handleJwt(admin);

			adminSignLogService.signAdmin(admin, SignResult.REFRESH, clientIp);

			return signJwts;
		} catch (Exception e) {
			adminSignLogService.registerSignLog(AdminSignLog.builder().clientIp(clientIp)
					.signResult(SignResult.REFRESH_FAIL).reason(e.getMessage()).build());
			throw e;
		}
	}

	/**
	 * 휴대폰 인증을 통해 관리자 계정 정보를 조회합니다.
	 * <p>
	 * 이메일 찾기의 프로세스의 일부로 사용되며, 제출된 휴대폰 인증 토큰을 검증하고, 토큰에 기록된 휴대폰 번호와 파라미터로 받은 번호가 일치하는지 확인합니다. <br>
	 * 모든 검증이 완료되면 해당 번호를 사용하는 관리자 중 **탈퇴(DELETE) 상태가 아닌 계정**을 찾아 DTO로 반환합니다. <br>
	 * 조회가 성공하면 사용된 인증 토큰은 즉시 삭제됩니다.
	 *
	 * @param phone                  조회할 관리자의 휴대폰 번호
	 * @param verificationPhoneToken 휴대폰 인증 성공 후 발급된 일회성 토큰
	 * 
	 * @return 조회된 관리자의 정보가 담긴 {@link AdminResponseDto}
	 * 
	 * @throws IllegalTokenException        토큰이 null이거나 비어있는 경우. ({@link TokenProvider#getDecryptedTokenDto})
	 * @throws NoSuchTokenException         Redis에 일치하는 토큰이 없는 경우. ({@link TokenProvider#getDecryptedTokenDto})
	 * @throws RedisGetException            Redis 데이터를 DTO로 변환 또는 복호화하는 데 실패한 경우. ({@link TokenProvider#getDecryptedTokenDto})
	 * @throws NoSuchDataException          DB에 해당 인증 요청 또는 관리자 정보가 없는 경우. ({@link VerificationService#isVerify}, {@link #findAdminWithValidateStatusByAdminId})
	 * @throws VerificationExpiredException 인증 유효 시간이 만료된 경우. ({@link VerificationService#isVerify})
	 * @throws NotVerifyException           아직 인증이 완료되지 않은 요청인 경우. ({@link VerificationService#isVerify})
	 * @throws IllegalRequestException      휴대폰 번호와 토큰에 기록된 휴대폰 번호가 다른 경우. ({@link VerificationPhoneTokenDto#comparePhone})
	 * @throws VersionMismatchException     데이터가 다른 곳에서 먼저 수정되어 버전이 일치하지 않는 경우. ({@link Admin#verifyVersion})
	 * 
	 * @apiNote 이 메서드 호출 시 발생할 수 있는 예외와 그에 따른 HTTP 응답 코드입니다.
	 *          <ul>
	 *          <li>Token Errors</li>
	 *          <li>{@code IllegalTokenException}: 400 Bad Request (Code: 4580)</li>
	 *          <li>{@code NoSuchTokenException}: 404 Not Found (Code: 4581)</li>
	 *          <li>{@code RedisGetException}: 500 Internal Server Error (Code: 5002)</li>
	 *          </ul>
	 *          <ul>
	 *          <li>Verification Errors</li>
	 *          <li>{@code NoSuchDataException} (인증 요청 없음): 404 Not Found (Code: 4590)</li>
	 *          <li>{@code VerificationExpiredException}: 403 Forbidden (Code: 4593)</li>
	 *          <li>{@code NotVerifyException}: 403 Forbidden (Code: 4594)</li>
	 *          </ul>
	 *          <ul>
	 *          <li>Request Errors</li>
	 *          <li>{@code IllegalRequestException} (관리자 없음): 400, Bad Request (Code: 4001)</li>
	 *          </ul>
	 *          <ul>
	 *          <li>Admin Account Errors</li>
	 *          <li>{@code NoSuchDataException} (관리자 없음): 404 Not Found (Code: 4530)</li>
	 *          </ul>
	 */
	public Admin getAdminByVerifiedPhone(String phone, String verificationPhoneToken) {

		VerificationPhoneTokenDto verificationPhoneTokenDto = tokenProvider
				.getDecryptedTokenDto(VerificationPhoneTokenDto.TOKENTYPE, verificationPhoneToken);
		verificationService.isVerify(verificationPhoneTokenDto.getIntVerificationId());
		verificationPhoneTokenDto.comparePhone(phone);

		Admin admin = adminRepository.findByPhoneAndAdminStatusNot(phone, AdminStatus.DELETE)
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND, "찾을 수 없는 유저."));

		tokenProvider.removeToken(TokenType.VERIFICATION_PHONE, verificationPhoneToken);
		return admin;
	}

	/**
	 * 이메일로 관리자를 조회하여 등록된 휴대폰 번호를 반환합니다.
	 * <p>
	 * 비밀번호 찾기 1차 프로세스로 사용되며, 이메일 주소를 기반으로 **탈퇴(DELETE) 상태가 아닌** 관리자 계정을 찾아 해당 계정의 휴대폰 번호를 반환합니다.
	 *
	 * @param email 조회할 관리자의 이메일 주소
	 * 
	 * @return 조회된 관리자의 휴대폰 번호
	 * 
	 * @throws NoSuchDataException 해당 이메일로 가입된 활성 계정이 없는 경우
	 * 
	 * @apiNote 이 메서드 호출 시 발생할 수 있는 예외와 그에 따른 HTTP 응답 코드입니다.
	 *          <li>Admin Account Errors</li>
	 *          <li>{@code NoSuchDataException} (관리자 없음): 404 Not Found (Code: 4530)</li>
	 *          </ul>
	 */
	public String getAdminPhoneByEmail(String email) {
		log.debug("* 이메일 통해 휴대폰번호 조회. email: {}", LogMaskingUtil.maskEmail(email, MaskLevel.MEDIUM));

		Admin admin = adminRepository.findByEmailAndAdminStatusNot(email, AdminStatus.DELETE)
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND, "가입되지 않은 이메일."));

		return admin.getPhone();
	}

	/**
	 * 비밀번호 찾기 2차 프로세스로 휴대폰 인증 통해 자격 검증
	 * 
	 * @param email
	 * @param phone
	 * @param verificationPhoneToken
	 * 
	 * @return
	 */
	public String createFindPwTokenByVerifyPhone(String email, String phone, String verificationPhoneToken) {

		VerificationPhoneTokenDto verificationPhoneTokenDto = tokenProvider
				.getDecryptedTokenDto(VerificationPhoneTokenDto.TOKENTYPE, verificationPhoneToken);
		verificationService.isVerify(verificationPhoneTokenDto.getIntVerificationId());

		Admin admin = adminRepository.findByEmailAndPhone(email, phone)
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND, "찾을 수 없는 유저."));
		admin.isStop();
		admin.isDelete();

		String findPwToken = tokenProvider.createToken(new FindAdminPwTokenDto(admin.getAdminId() + "", email),
				FindAdminPwTokenDto.TOKENTYPE);

		tokenProvider.removeToken(TokenType.VERIFICATION_PHONE, verificationPhoneToken);

		return findPwToken;
	}

	/**
	 * 비밀번호 찾기 2차 프로세스로 이메일 인증 통해 자격 검증
	 * 
	 * @param email
	 * @param verificationEmailToken
	 * 
	 * @return
	 */
	public String createFindPwTokenByVerifyEmail(String email, String verificationEmailToken) {

		VerificationEmailTokenDto verificationEmailTokenDto = tokenProvider
				.getDecryptedTokenDto(VerificationEmailTokenDto.TOKENTYPE, verificationEmailToken);
		verificationService.isVerify(verificationEmailTokenDto.getIntVerificationId());

		Admin admin = adminRepository.findByEmail(email)
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND, "찾을 수 없는 유저."));
		admin.isStop();
		admin.isDelete();

		String findPwToken = tokenProvider.createToken(new FindAdminPwTokenDto(admin.getAdminId() + "", email),
				FindAdminPwTokenDto.TOKENTYPE);

		tokenProvider.removeToken(TokenType.VERIFICATION_EMAIL, verificationEmailToken);

		return findPwToken;
	}

	/**
	 * 비밀번호 찾기 마지막 프로세스로 비밀번호 변경
	 * 
	 * @param token
	 * @param newPassword
	 * @param clientIp
	 */
	@SuppressWarnings("incomplete-switch")
	@Transactional
	public void updatePasswordByFindPwToken(String token, String newPassword, String confirmNewPassword, String clientIp) {
		
		if (!Objects.equals(newPassword, confirmNewPassword)) {
			throw new IllegalRequestException(ILLEGAL_INPUT_VALUE,
					"새로운 두 비밀번호가 일치하지 않음. newPassword: " + newPassword + ", confirmNewPassword: " + confirmNewPassword);
		}

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
		adminSignLogService.signAdminAndFailLogResolve(admin, SignResult.UPDATE_PASSWORD, clientIp);

		tokenProvider.removeToken(TokenType.ACCESS_FINDPW, token);
	}

	private Admin findAdminWithValidateStatusByAdminId(int adminId) {
		Admin admin = adminRepository.findById(adminId)
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND, "찾을 수 없는 유저."));
		admin.validateStatus();
		return admin;
	}

	private Admin findAdminWithValidateStatusByEmail(String email) {
		Admin admin = adminRepository.findByEmail(email)
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND, "찾을 수 없는 유저."));
		admin.validateStatus();
		return admin;
	}

	/**
	 * 비밀번호 검증 및 결과 `LoginLog` 기록
	 * 
	 * @param admin
	 * @param reqPassword
	 * @param clientIp
	 */
	private void validatePassword(Admin admin, String rawPassword, String clientIp) {
		if (!passwordEncoder.matches(rawPassword, admin.getPassword())) {
			adminSignLogService.signAdmin(admin, SignResult.INVALID_PASSWORD, clientIp);
			int signFailCount;
			try {
				signFailCount = adminSignLogService.validSignFailCount(admin.getAdminId() + "");
			} catch (TooManySignFailException e) {
				admin.setAdminStatus(AdminStatus.STAY);
				adminRepository.save(admin);
				log.warn("비밀번호 불일치 10회로 계정 일시정지: email={}, ip={}", admin.getEmail(), clientIp);
				throw e;
			}
			throw new MismatchPasswordException(SIGNIN_FAILED, "비밀번호 불일치. 실패 횟수: " + signFailCount);
		}
	}

	/**
	 * Admin 정보 통해 로그인 토큰들 발급 및 RefreshToken은 Redis 저장
	 * 
	 * @param admin
	 * 
	 * @return
	 */
	private Map<String, String> handleJwt(Admin admin) {
		Map<String, String> signToken = jwtProvider.createSignToken(admin.getAdminId(), List.of("ROLE_ADMIN"),
				Map.of("email", admin.getEmail(), "name", admin.getName()));

		tokenProvider.addRefreshJwt(admin.getAdminId(), signToken.get("refreshToken"));
		return signToken;
	}

}
