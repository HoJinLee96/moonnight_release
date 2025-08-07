package net.chamman.moonnight.auth.sign;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.token.JwtProvider;
import net.chamman.moonnight.auth.token.TokenProvider;
import net.chamman.moonnight.auth.token.TokenProvider.TokenType;
import net.chamman.moonnight.auth.token.dto.VerificationEmailTokenDto;
import net.chamman.moonnight.auth.token.dto.VerificationPhoneTokenDto;
import net.chamman.moonnight.auth.verification.VerificationService;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.global.exception.crypto.DecryptException;
import net.chamman.moonnight.global.exception.crypto.EncryptException;
import net.chamman.moonnight.global.exception.jwt.CreateJwtException;
import net.chamman.moonnight.global.exception.jwt.IllegalJwtException;
import net.chamman.moonnight.global.exception.jwt.TimeOutJwtException;
import net.chamman.moonnight.global.exception.jwt.ValidateJwtException;
import net.chamman.moonnight.global.exception.redis.RedisGetException;
import net.chamman.moonnight.global.exception.redis.RedisSetException;
import net.chamman.moonnight.global.exception.token.IllegalTokenException;
import net.chamman.moonnight.global.exception.token.NoSuchTokenException;
import net.chamman.moonnight.global.exception.verification.NotVerifyException;
import net.chamman.moonnight.global.exception.verification.VerificationExpiredException;

@Service
@Slf4j
@RequiredArgsConstructor
public class SignService {

	private final VerificationService verificationService;
	private final JwtProvider jwtProvider;
	private final TokenProvider tokenProvider;

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

		tokenProvider.removeToken(TokenType.VERIFICATION_EMAIL, verificationEmailToken);

		return authTokenDto;
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

}
