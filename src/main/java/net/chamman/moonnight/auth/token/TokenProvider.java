package net.chamman.moonnight.auth.token;

import static net.chamman.moonnight.global.exception.HttpStatusCode.TOKEN_GET_FIAL;
import static net.chamman.moonnight.global.exception.HttpStatusCode.TOKEN_ILLEGAL;
import static net.chamman.moonnight.global.exception.HttpStatusCode.TOKEN_NOT_FOUND;
import static net.chamman.moonnight.global.exception.HttpStatusCode.TOKEN_SET_FIAL;
import static net.chamman.moonnight.global.exception.HttpStatusCode.TOKEN_VALUE_MISMATCH;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.AesProvider;
import net.chamman.moonnight.auth.crypto.Encryptable;
import net.chamman.moonnight.auth.token.dto.AdminPasswordConfirmTokenDto;
import net.chamman.moonnight.auth.token.dto.AdminSignUpTokenDto;
import net.chamman.moonnight.auth.token.dto.FindAdminPwTokenDto;
import net.chamman.moonnight.auth.token.dto.VerificationEmailTokenDto;
import net.chamman.moonnight.auth.token.dto.VerificationPhoneTokenDto;
import net.chamman.moonnight.global.exception.redis.RedisGetException;
import net.chamman.moonnight.global.exception.redis.RedisSetException;
import net.chamman.moonnight.global.exception.token.IllegalTokenException;
import net.chamman.moonnight.global.exception.token.NoSuchTokenException;
import net.chamman.moonnight.global.exception.token.TokenValueMismatchException;
import net.chamman.moonnight.global.util.LogMaskingUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil.MaskLevel;

@Component
@Slf4j
@RequiredArgsConstructor
public class TokenProvider {

	private final AesProvider aesProvider;
	private final RedisTemplate<String, String> redisTemplate;
	private ObjectMapper objectMapper = new ObjectMapper();

	public enum TokenType {
		VERIFICATION_PHONE("verification:phone:", Duration.ofMinutes(5), VerificationPhoneTokenDto.class),
		VERIFICATION_EMAIL("verification:email:", Duration.ofMinutes(5), VerificationEmailTokenDto.class),
		ACCESS_FINDPW("access:findpw:", Duration.ofMinutes(5), FindAdminPwTokenDto.class),
		ACCESS_PASSWORD("access:password:", Duration.ofMinutes(5), AdminPasswordConfirmTokenDto.class),
		ACCESS_SIGNUP("access:signup:", Duration.ofMinutes(10), AdminSignUpTokenDto.class),
		JWT_REFRESH("jwt:refresh:", Duration.ofDays(14), null), JWT_BLACKLIST("jwt:blacklist:", null, null);

		private final String prefix;
		private final Duration ttl;
		private final Class<? extends Encryptable<?>> dtoType;

		TokenType(String prefix, Duration ttl, Class<? extends Encryptable<?>> dtoType) {
			this.prefix = prefix;
			this.ttl = ttl;
			this.dtoType = dtoType;
		}

		public String getPrefix() {
			return prefix;
		}

		public Duration getTtl() {
			return ttl;
		}

		public Class<? extends Encryptable<?>> getDtoType() {
			return dtoType;
		}
	}

	/**
	 * DTO를 페이로드로 사용하는 토큰 생성
	 * 
	 * @param <T>       Encryptable를 구현한 DTO 타입
	 * @param dto       토큰에 담을 데이터 DTO 객체
	 * @param tokenType 토큰 타입
	 * 
	 * @return 토큰
	 * 
	 * @throws RedisSetException {@link #createToken} Redis 저장 실패
	 */
	public <T extends Encryptable<T>> String createToken(T dto, TokenType tokenType) {
		log.debug("*{} 타입 토큰 발행.", tokenType);

		if (dto.getClass() != tokenType.getDtoType()) {
			throw new RedisSetException(TOKEN_SET_FIAL, "TokenType과 Dto 타입이 일치하지 않습니다. " + "입력 받은 타입: "
					+ tokenType.getDtoType().getSimpleName() + ", 입력 받은 Dto 타입: " + dto.getClass().getSimpleName());
		}

		String token = UUID.randomUUID().toString();

		try {
			// 1. DTO에 정의된 방식대로 암호화 수행
			T encryptedDto = dto.encrypt(aesProvider);

			// 2. 암호화된 DTO 객체를 JSON 문자열로 직렬화
			String json = objectMapper.writeValueAsString(encryptedDto);

			// 3. Redis에 저장
			redisTemplate.opsForValue().set(tokenType.getPrefix() + token, json, tokenType.getTtl());

		} catch (Exception e) {
			throw new RedisSetException(TOKEN_SET_FIAL, tokenType.name() + " Token Redis 저장 중 오류. " + e.getMessage(),
					e);
		}

		return token;
	}

	/**
	 * Redis에 저장된 암호화된 토큰을 조회하고, 복호화하여 DTO 객체로 반환합니다.
	 * 
	 * 토큰 문자열을 이용해 Redis에서 암호화된 JSON 데이터를 가져온 후, 지정된 DTO 타입으로 변환하고 AES 복호화를 수행합니다.
	 *
	 * @param <T>       복호화 가능한 데이터를 담을 DTO 타입 (Encryptable 인터페이스 구현체)
	 * @param tokenType 조회할 토큰의 종류 (e.g., 이메일 인증, 휴대폰 인증 등)
	 * @param token     조회 및 검증할 토큰 문자열 (일반적으로 UUID)
	 * 
	 * @return 복호화된 데이터가 담긴 DTO 객체
	 * 
	 * @throws IllegalTokenException 토큰 null 또는 비어있음 {@link #getToken}
	 * @throws NoSuchTokenException  일치하는 토큰 없음 {@link #getToken}
	 * @throws RedisGetException     Redis에서 가져온 데이터를 DTO로 변환(역직렬화)하거나 복호화하는 데 실패한 경우 {@link #getDecryptedTokenDto}
	 * 
	 * @apiNote 이 메서드 호출 시 발생할 수 있는 예외와 그에 따른 HTTP 응답 코드입니다.
	 *          <ul>
	 *          <li>Token Errors</li>
	 *          <li>{@code IllegalTokenException}: 400 Bad Request (Code: 4580)</li>
	 *          <li>{@code NoSuchTokenException}: 404 Not Found (Code: 4581)</li>
	 *          <li>{@code RedisGetException}: 500 Internal Server Error (Code: 5002)</li>
	 *          </ul>
	 */
	public <T extends Encryptable<T>> T getDecryptedTokenDto(TokenType tokenType, String token) {
		log.debug("* {} 타입 토큰 검증. Token: [{}]", tokenType, LogMaskingUtil.maskToken(token, MaskLevel.MEDIUM));

		String json = getToken(tokenType, token);

		try {

			@SuppressWarnings("unchecked")
			T encryptedDto = (T) objectMapper.readValue(json, tokenType.getDtoType());

			return encryptedDto.decrypt(aesProvider);

		} catch (Exception e) {
			throw new RedisGetException(TOKEN_GET_FIAL, "Redis에서 토큰 조회 중 오류. " + e.getMessage(), e);
		}
	}

	/**
	 * 리프레쉬토큰 Redis set
	 * 
	 * @param userId
	 * @param refreshToken
	 * 
	 * @throws RedisSetException {@link #addRefreshJwt} 리프레쉬 토큰 Redis 저장 실패
	 */
	public void addRefreshJwt(int userId, String refreshToken) {
		log.debug("*RefreshToken Redis에 저장. RefreshToken: [{}]",
				LogMaskingUtil.maskToken(refreshToken, MaskLevel.MEDIUM));

		try {
			redisTemplate.opsForValue().set(TokenType.JWT_REFRESH.getPrefix() + userId, refreshToken,
					TokenType.JWT_REFRESH.getTtl());
		} catch (Exception e) {
			throw new RedisSetException(TOKEN_SET_FIAL, "RefreshToken Redis 저장 중 오류. " + e.getMessage(), e);
		}
	}

	/**
	 * 액세스토큰 블랙리스트 Redis set
	 * 
	 * @param accessToken
	 * @param ttl
	 * @param result
	 * 
	 * @throws RedisSetException {@link #addAccessJwtBlacklist} Redis 저장 중 오류
	 */
	public void addTokenBlacklist(String token, long ttl, String result) {
		log.debug("* Token 블랙리스트에 저장. Token: [{}], ttl: [{}], Result: [{}]",
				LogMaskingUtil.maskToken(token, MaskLevel.MEDIUM), ttl, result);

		try {
			redisTemplate.opsForValue().set(TokenType.JWT_BLACKLIST.getPrefix() + token, result,
					Duration.ofMillis(ttl));
		} catch (Exception e) {
			throw new RedisSetException(TOKEN_SET_FIAL, "Token BlackList Redis 저장 중 오류. " + e.getMessage(), e);
		}
	}

	/**
	 * 블랙리스트 조회
	 * 
	 * @param accessToken
	 * 
	 * @return Redis 조회 value 또는 null
	 */
	public String getBlackListValue(String token) {
		log.debug("* 블랙리스트 조회. Token: [{}]", LogMaskingUtil.maskToken(token, MaskLevel.MEDIUM));

		return redisTemplate.opsForValue().get(TokenType.JWT_BLACKLIST.getPrefix() + token);
	}

	/**
	 * Redis에 토큰 존재 여부 확인
	 * 
	 * @param type
	 * @param key
	 * 
	 * @return 존재 여부
	 */
	public boolean isValid(TokenType type, String key) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(type.getPrefix() + key));
	}

	/**
	 * 토큰 삭제
	 * 
	 * @param type
	 * @param key
	 * 
	 * @return 삭제 여부
	 */
	public boolean removeToken(TokenType type, String key) {
		log.debug("* Redis에서 {} 타입 토큰 삭제 조회. Key: [{}]", type, LogMaskingUtil.maskToken(key, MaskLevel.MEDIUM));

		return redisTemplate.delete(type.getPrefix() + key);
	}

	/**
	 * 입력받은 RefreshToken과 Redis의 저장되어있는 RefreshToken이 일치한지 검사.
	 * 
	 * @param reqUserId
	 * @param token
	 * 
	 * @return 일치 여부
	 * 
	 * @throws NoSuchTokenException        {@link #validRefreshToken} Redis에 없는 키
	 * @throws TokenValueMismatchException {@link #validRefreshToken} Redis 값과 비교 불일치
	 */
	public boolean validRefreshToken(String userId, String reqRefreshToken) {
		log.debug("* RefreshToken 검증. RefreshToken: [{}]", LogMaskingUtil.maskToken(reqRefreshToken, MaskLevel.MEDIUM));

		String path = TokenType.JWT_REFRESH.getPrefix() + userId;
		String refreshToken = redisTemplate.opsForValue().get(path);
		if (refreshToken == null) {
			throw new NoSuchTokenException(TOKEN_NOT_FOUND, userId + "에 일치하는 리프레쉬 토큰 없음.");
		}
		if (!Objects.equals(reqRefreshToken, refreshToken)) {
			throw new TokenValueMismatchException(TOKEN_VALUE_MISMATCH,
					"Redis 값과 비교 불일치. reqRefreshToken: " + reqRefreshToken + ", refreshToken: " + refreshToken);
		}
		return true;
	}

	/**
	 * Redis에서 토큰 조회
	 * 
	 * @param tokenType
	 * @param token
	 * 
	 * @return json형식 String
	 * 
	 * @throws IllegalTokenException 토큰 null 또는 비어있음 {@link #getToken}
	 * @throws NoSuchTokenException  일치하는 토큰 없음 {@link #getToken}
	 * 
	 * @apiNote 이 메서드 호출 시 발생할 수 있는 예외와 그에 따른 HTTP 응답 코드입니다.
	 *          <ul>
	 *          <li>Token Errors</li>
	 *          <li>{@code IllegalTokenException}: 400 Bad Request (Code: 4580)</li>
	 *          <li>{@code NoSuchTokenException}: 404 Not Found (Code: 4581)</li>
	 *          </ul>
	 */
	private String getToken(TokenType tokenType, String token) {
		log.debug("* Redis에 {} 토큰 조회. Token: [{}]", tokenType, LogMaskingUtil.maskToken(token, MaskLevel.MEDIUM));

		if (token == null || token.isBlank()) {
			throw new IllegalTokenException(TOKEN_ILLEGAL, tokenType.name() + " Token null 또는 비어있음.");
		}
		String key = tokenType.getPrefix() + token;
		String json = redisTemplate.opsForValue().get(key);

		if (json == null) {
			throw new NoSuchTokenException(TOKEN_NOT_FOUND, tokenType.name() + " 일치하는 토큰이 없음.");
		}
		return json;
	}

}