package net.chamman.moonnight.domain.admin;

import static net.chamman.moonnight.global.exception.HttpStatusCode.PHONE_ALREADY_EXISTS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.USER_NOT_FOUND;

import java.util.Objects;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.token.TokenProvider;
import net.chamman.moonnight.auth.token.TokenProvider.TokenType;
import net.chamman.moonnight.auth.token.dto.VerificationEmailTokenDto;
import net.chamman.moonnight.auth.token.dto.VerificationPhoneTokenDto;
import net.chamman.moonnight.auth.verification.VerificationService;
import net.chamman.moonnight.domain.admin.Admin.AdminStatus;
import net.chamman.moonnight.domain.admin.dto.AdminNameRequestDto;
import net.chamman.moonnight.domain.admin.dto.AdminPasswordUpdateRequestDto;
import net.chamman.moonnight.domain.admin.dto.AdminPhoneRequestDto;
import net.chamman.moonnight.global.exception.HttpStatusCode;
import net.chamman.moonnight.global.exception.IllegalRequestException;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.global.exception.VersionMismatchException;
import net.chamman.moonnight.global.exception.admin.DuplicationException;
import net.chamman.moonnight.global.exception.redis.RedisGetException;
import net.chamman.moonnight.global.exception.status.StatusDeleteException;
import net.chamman.moonnight.global.exception.status.StatusStayException;
import net.chamman.moonnight.global.exception.status.StatusStopException;
import net.chamman.moonnight.global.exception.token.IllegalTokenException;
import net.chamman.moonnight.global.exception.token.NoSuchTokenException;
import net.chamman.moonnight.global.exception.verification.NotVerifyException;
import net.chamman.moonnight.global.exception.verification.VerificationExpiredException;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminService {

	private final AdminRepository adminRepository;
	private final VerificationService verificationService;
	private final TokenProvider tokenProvider;
	private final PasswordEncoder passwordEncoder;

	/**
	 * 지정된 ID를 가진 활성 상태의 관리자 정보를 조회합니다.
	 * 
	 * 내부적으로 관리자를 조회한 후, 해당 관리자의 상태가 정상(ACTIVE)인지 검증합니다. 계정이 일시 정지, 정지, 탈퇴 등 비활성 상태일 경우 각각에 맞는 예외를 발생시킵니다.
	 *
	 * @param adminId 조회할 관리자의 고유 ID
	 * 
	 * @return 조회된 활성 상태의 Admin 객체
	 * 
	 * @throws NoSuchDataException   해당 ID의 관리자가 존재하지 않을 경우.
	 * @throws StatusStayException   계정이 '일시 정지' 상태일 경우.
	 * @throws StatusStopException   계정이 '정지' 상태일 경우.
	 * @throws StatusDeleteException 계정이 '탈퇴' 상태일 경우.
	 * 
	 * @apiNote 이 메서드 호출 시 발생할 수 있는 예외와 그에 따른 HTTP 응답 코드입니다.
	 *          <ul>
	 *          <li>Admin Account Errors</li>
	 *          <li>{@code NoSuchDataException} (관리자 없음): 404 Not Found (Code: 4530)</li>
	 *          <li>{@code StatusStayException}: 403 Forbidden (Code: 4533)</li>
	 *          <li>{@code StatusStopException}: 401 Unauthorized (Code: 4534)</li>
	 *          <li>{@code StatusDeleteException}: 410 Gone (Code: 4535)</li>
	 *          <li>{@code VersionMismatchException}: 409 Conflict (Code: 4540)</li>
	 *          </ul>
	 */
	public Admin getActiveAdminById(int adminId) {
		return findAdminWithValidateStatusByAdminId(adminId);
	}

	/**
	 * 이메일 문자열과 일치하는 활성 상태의 관리자 정보를 조회합니다.
	 * 
	 * 내부적으로 관리자를 조회한 후, 해당 관리자의 상태가 정상(ACTIVE)인지 검증합니다. 계정이 일시 정지, 정지, 탈퇴 등 비활성 상태일 경우 각각에 맞는 예외를 발생시킵니다.
	 *
	 * @param email 조회할 관리자의 email
	 * 
	 * @return 조회된 활성 상태의 Admin 객체
	 * 
	 * @throws NoSuchDataException   해당 ID의 관리자가 존재하지 않을 경우.
	 * @throws StatusStayException   계정이 '일시 정지' 상태일 경우.
	 * @throws StatusStopException   계정이 '정지' 상태일 경우.
	 * @throws StatusDeleteException 계정이 '탈퇴' 상태일 경우.
	 * 
	 * @apiNote 이 메서드 호출 시 발생할 수 있는 예외와 그에 따른 HTTP 응답 코드입니다.
	 *          <ul>
	 *          <li>Admin Account Errors</li>
	 *          <li>{@code NoSuchDataException} (관리자 없음): 404 Not Found (Code: 4530)</li>
	 *          <li>{@code StatusStayException}: 403 Forbidden (Code: 4533)</li>
	 *          <li>{@code StatusStopException}: 401 Unauthorized (Code: 4534)</li>
	 *          <li>{@code StatusDeleteException}: 410 Gone (Code: 4535)</li>
	 *          <li>{@code VersionMismatchException}: 409 Conflict (Code: 4540)</li>
	 *          </ul>
	 */
	public Admin getActiveAdminByEmail(String email) {
		return findAdminWithValidateStatusByEmail(email);
	}

	/**
	 * 비밀번호 변경
	 * 
	 * @param token
	 * @param newPassword
	 * @param clientIp
	 */
	@Transactional
	public void updatePasswordByEmail(int adminId, AdminPasswordUpdateRequestDto dto, String token) {
	
		VerificationEmailTokenDto tokenDto = tokenProvider.getDecryptedTokenDto(TokenType.VERIFICATION_EMAIL, token);
		verificationService.isVerify(tokenDto.getIntVerificationId());
		
		if (!Objects.equals(dto.newPassword(), dto.confirmNewPassword())) {
			throw new IllegalRequestException(HttpStatusCode.REQUEST_BODY_NOT_VALID);
		}
	
		Admin admin = findAdminWithValidateStatusByAdminId(adminId);
		admin.verifyVersion(dto.version());
	
		admin.setPassword(passwordEncoder.encode(dto.newPassword()));
	}
	
	/**
	 * 비밀번호 변경
	 * 
	 * @param token
	 * @param newPassword
	 * @param clientIp
	 */
	@Transactional
	public void updatePasswordByPhone(int adminId, AdminPasswordUpdateRequestDto dto, String token) {
		
		VerificationPhoneTokenDto tokenDto = tokenProvider.getDecryptedTokenDto(TokenType.VERIFICATION_PHONE, token);
		verificationService.isVerify(tokenDto.getIntVerificationId());
		
		if (!Objects.equals(dto.newPassword(), dto.confirmNewPassword())) {
			throw new IllegalRequestException(HttpStatusCode.REQUEST_BODY_NOT_VALID);
		}
		
		Admin admin = findAdminWithValidateStatusByAdminId(adminId);
		admin.verifyVersion(dto.version());
		
		admin.setPassword(passwordEncoder.encode(dto.newPassword()));
	}

	/**
	 * 휴대폰 인증 완료 후, 관리자의 휴대폰 번호를 변경합니다.
	 * 
	 * 제출된 인증 토큰을 복호화하고 유효성을 검증합니다. DB에서 인증 요청 기록을 찾아 정상 처리되었는지 재차 확인합니다. 관리자 계정이 활성 상태인지 확인합니다. 데이터 정합성을 위해 엔티티 버전을 검증합니다. 모든 검증이 통과되면 휴대폰 번호를 변경하고, 사용된 인증 토큰을 삭제합니다.
	 *
	 * @param adminId                휴대폰 번호를 변경할 관리자의 고유 ID
	 * @param dto                    AdminPhoneRequestDto 새로 변경할 휴대폰 번호와 데이터 정합성 검증을 위한 엔티티 버전 번호
	 * @param verificationPhoneToken 휴대폰 인증 성공 후 발급된 일회성 토큰
	 * 
	 * @throws IllegalTokenException        토큰이 null이거나 비어있는 경우. ({@link TokenProvider#getDecryptedTokenDto})
	 * @throws NoSuchTokenException         Redis에 일치하는 토큰이 없는 경우. ({@link TokenProvider#getDecryptedTokenDto})
	 * @throws RedisGetException            Redis 데이터를 DTO로 변환 또는 복호화하는 데 실패한 경우. ({@link TokenProvider#getDecryptedTokenDto})
	 * @throws NoSuchDataException          DB에 해당 인증 요청 또는 관리자 정보가 없는 경우. ({@link VerificationService#isVerify}, {@link #findAdminWithValidateStatusByAdminId})
	 * @throws VerificationExpiredException 인증 유효 시간이 만료된 경우. ({@link VerificationService#isVerify})
	 * @throws NotVerifyException           아직 인증이 완료되지 않은 요청인 경우. ({@link VerificationService#isVerify})
	 * @throws StatusStayException          계정이 '일시 정지' 상태일 경우. ({@link #findAdminWithValidateStatusByAdminId})
	 * @throws StatusStopException          계정이 '정지' 상태일 경우. ({@link #findAdminWithValidateStatusByAdminId})
	 * @throws StatusDeleteException        계정이 '탈퇴' 상태일 경우. ({@link #findAdminWithValidateStatusByAdminId})
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
	 *          <li>Admin Account Errors</li>
	 *          <li>{@code NoSuchDataException} (관리자 없음): 404 Not Found (Code: 4530)</li>
	 *          <li>{@code StatusStayException}: 403 Forbidden (Code: 4533)</li>
	 *          <li>{@code StatusStopException}: 401 Unauthorized (Code: 4534)</li>
	 *          <li>{@code StatusDeleteException}: 410 Gone (Code: 4535)</li>
	 *          <li>{@code VersionMismatchException}: 409 Conflict (Code: 4540)</li>
	 *          </ul>
	 */
	@Transactional
	public void updatePhoneByVerifiedPhone(int adminId, AdminPhoneRequestDto dto, String verificationPhoneToken) {

		VerificationPhoneTokenDto verificationPhoneTokenDto = tokenProvider
				.getDecryptedTokenDto(VerificationPhoneTokenDto.TOKENTYPE, verificationPhoneToken);
		verificationService.isVerify(verificationPhoneTokenDto.getIntVerificationId());

		Optional<Admin> existAdmin = adminRepository.findByPhoneAndAdminStatusNot(dto.phone(), AdminStatus.DELETE);
		if (existAdmin.isPresent()) {
			throw new DuplicationException(PHONE_ALREADY_EXISTS);
		}
		
		Admin admin = findAdminWithValidateStatusByAdminId(adminId);
		admin.verifyVersion(dto.version());
		admin.modify(null, dto.phone());

		tokenProvider.removeToken(TokenType.VERIFICATION_PHONE, verificationPhoneToken);
	}

	/**
	 * 관리자의 이름을 변경합니다.
	 * 
	 * @param adminId 이름을 변경할 관리자의 고유 ID
	 * @param name    새로 변경할 이름
	 * @param version 데이터 정합성 검증을 위한 엔티티 버전 번호
	 * 
	 * @throws NoSuchDataException      해당 ID의 관리자가 존재하지 않을 경우. {@link #findAdminWithValidateStatusByAdminId})
	 * @throws StatusStayException      계정이 '일시 정지' 상태일 경우. ({@link #findAdminWithValidateStatusByAdminId})
	 * @throws StatusStopException      계정이 '정지' 상태일 경우. ({@link #findAdminWithValidateStatusByAdminId})
	 * @throws StatusDeleteException    계정이 '탈퇴' 상태일 경우. ({@link #findAdminWithValidateStatusByAdminId})
	 * @throws VersionMismatchException 데이터가 다른 곳에서 먼저 수정되어 버전이 일치하지 않는 경우. ({@link Admin#verifyVersion})
	 * 
	 * @apiNote 이 메서드 호출 시 발생할 수 있는 예외와 그에 따른 HTTP 응답 코드입니다.
	 *          <li>Admin Account Errors</li>
	 *          <li>{@code NoSuchDataException} (관리자 없음): 404 Not Found (Code: 4530)</li>
	 *          <li>{@code StatusStayException}: 403 Forbidden (Code: 4533)</li>
	 *          <li>{@code StatusStopException}: 401 Unauthorized (Code: 4534)</li>
	 *          <li>{@code StatusDeleteException}: 410 Gone (Code: 4535)</li>
	 *          <li>{@code VersionMismatchException}: 409 Conflict (Code: 4540)</li>
	 *          </ul>
	 */
	@Transactional
	public void updateName(int adminId, AdminNameRequestDto dto) {

		Admin admin = findAdminWithValidateStatusByAdminId(adminId);
		admin.verifyVersion(dto.version());
		admin.modify(dto.name(), null);
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

}