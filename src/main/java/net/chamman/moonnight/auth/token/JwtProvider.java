package net.chamman.moonnight.auth.token;

import static net.chamman.moonnight.global.exception.HttpStatusCode.JWT_CREATE_FIAL;
import static net.chamman.moonnight.global.exception.HttpStatusCode.JWT_EXPIRED;
import static net.chamman.moonnight.global.exception.HttpStatusCode.JWT_VALIDATE_FIAL;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.AesProvider;
import net.chamman.moonnight.global.exception.crypto.DecryptException;
import net.chamman.moonnight.global.exception.jwt.CreateJwtException;
import net.chamman.moonnight.global.exception.jwt.TimeOutJwtException;
import net.chamman.moonnight.global.exception.jwt.ValidateJwtException;
import net.chamman.moonnight.global.util.LogMaskingUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil.MaskLevel;

@Component
@Slf4j
@PropertySource("classpath:application.properties")
public class JwtProvider {
	
	private final AesProvider aesProvider;
	private final Key signAccessHmacShaKey;
	private final Key signRefreshHmacShaKey;
	private final Key authHmacShaKey;
	
	private final long expiration14Days = 1000 * 60 * 60 * 24 * 14; // 14
	private final long expiration1Hour = 1000 * 60 * 60; // 1시간
	private final long expiration30Minute = 1000 * 60 * 30; // 30분
	
	public JwtProvider(
			@Autowired AesProvider aesProvider,
			@Value("${jwt.sign.access.secretKey}") String signAccessSecretKey,
			@Value("${jwt.sign.refresh.secretKey}") String signRefreshSecretKey,
			@Value("${jwt.auth.secretKey}") String authHmacShaKey
			) {
		this.aesProvider = aesProvider;
		this.signAccessHmacShaKey = Keys.hmacShaKeyFor(signAccessSecretKey.getBytes(StandardCharsets.UTF_8));
		this.signRefreshHmacShaKey = Keys.hmacShaKeyFor(signRefreshSecretKey.getBytes(StandardCharsets.UTF_8));
		this.authHmacShaKey = Keys.hmacShaKeyFor(authHmacShaKey.getBytes(StandardCharsets.UTF_8));
	}
	
	/**
	 * @param userId
	 * @param roles
	 * @param claims
	 * @throws CreateJwtException {@link #createAccessToken}, {@link #createRefreshToken} 토큰 생성 실패
	 * @return 액세스토큰, 리프레쉬토큰
	 */
	public Map<String,String> createSignToken(int userId, List<String> roles, Map<String, Object> claims) {
		String accessToken = createAccessToken(userId, roles, claims);
		String refreshToken = createRefreshToken(userId);
//		log.debug("* 로그인 토큰 발행 완료. accessToken: [{}], refreshToken: [{}]", accessToken, refreshToken);
		return Map.of("accessToken",accessToken,"refreshToken",refreshToken);
	}
	
	/** 액세스 토큰 생성
	 * @param userId
	 * @param roles
	 * @param claims
	 * @return 액세스 토큰
	 * 
	 * @throws CreateJwtException {@link #createAccessToken} 토큰 생성 실패
	 */
	private String createAccessToken(int userId, List<String> roles, Map<String, Object> claims) {
		log.debug("* AccessToken 발행. UserID: [{}], Roles: [{}]", LogMaskingUtil.maskId(userId, MaskLevel.MEDIUM), roles.get(0));
		
		try {
			JwtBuilder builder = Jwts.builder()
					.setSubject(aesProvider.encrypt(String.valueOf(userId)))
					.setIssuedAt(new Date())
					.setExpiration(new Date(System.currentTimeMillis() + expiration1Hour))
					.signWith(signAccessHmacShaKey, SignatureAlgorithm.HS256);
			
			if (claims != null) {
				for (Map.Entry<String, Object> entry : claims.entrySet()) {
					Object value = entry.getValue();
					if (value instanceof String strVal) {
						builder.claim(entry.getKey(), aesProvider.encrypt(strVal));
					} else {
						builder.claim(entry.getKey(), value); // 예외 방지
					}
				}
			}
			builder.claim("roles",roles);
			return builder.compact();
		} catch (Exception e) {
			throw new CreateJwtException(JWT_CREATE_FIAL,"AccessToken 생성 실패. "+e.getMessage(), e);
		}
	}
	
	/** 리프레쉬 토큰 생성
	 * @param userId
	 * @return 리프레쉬 토큰
	 * 
	 * @throws CreateJwtException {@link #createRefreshToken} 토큰 생성 실패
	 */
	private String createRefreshToken(int userId) {
		log.debug("* RefreshToken 발행. UserID: [{}]", LogMaskingUtil.maskId(userId, MaskLevel.MEDIUM));
		
		try {
			return Jwts.builder()
					.setSubject(aesProvider.encrypt(userId + ""))
					.setExpiration(new Date(System.currentTimeMillis() + expiration14Days))
					.signWith(signRefreshHmacShaKey, SignatureAlgorithm.HS256)
					.compact();
		} catch (Exception e) {
			throw new CreateJwtException(JWT_CREATE_FIAL,"RefreshToken 생성 실패. "+e.getMessage(),e);
		}
	}
	
	/** 휴대폰,이메일 인증 로그인 토큰 생성
	 * @param verificationId
	 * @param claims
	 * @return 휴대폰 인증 로그인 토큰
	 * 
	 * @throws CreateJwtException {@link #createAuthToken} 토큰 생성 실패
	 */
	public String createAuthToken(String verificationId, String recipient) {
		log.debug("* AuthToken 발행. VerificationId: [{}], recipient: [{}]", 
				LogMaskingUtil.maskId(verificationId, MaskLevel.MEDIUM),
				LogMaskingUtil.maskRecipient(recipient, MaskLevel.MEDIUM)
				);
		
		try {
			
			return Jwts.builder()
					.setSubject(aesProvider.encrypt(verificationId)) 
					.setIssuedAt(new Date())
					.setExpiration(new Date(System.currentTimeMillis() + expiration30Minute))
					.signWith(authHmacShaKey, SignatureAlgorithm.HS256)
					.claim("recipient", aesProvider.encrypt(recipient))
					.claim("roles",List.of("RULE_AUTH"))
					.compact();
			
		} catch (Exception e) {
			throw new CreateJwtException(JWT_CREATE_FIAL,"AuthToken 생성 실패. "+e.getMessage(),e);
		}
	}
	
	
	/** 액세스 토큰 검증
	 * @param token
	 * @return 복호화된 유저 정보 Claims
	 * 
	 * @throws TimeOutJwtException {@link #validateAccessToken} 시간 초과
	 * @throws ValidateJwtException {@link #validateAccessToken} JWT 파싱 실패
	 */
	public Map<String, Object> validateAccessToken(String token) {
		log.debug("* AccessToken 검증. AccessToken: [{}]", LogMaskingUtil.maskToken(token, MaskLevel.MEDIUM));
		
		try {
			Claims claims = Jwts.parserBuilder()
					.setSigningKey(signAccessHmacShaKey)
					.build()
					.parseClaimsJws(token)
					.getBody();
			
			return getDecryptedClaims(claims);
		} catch (ExpiredJwtException e) {
			throw new TimeOutJwtException(JWT_EXPIRED, "* AccessToken 시간 만료.", e);
		} catch (Exception e) {
			throw new ValidateJwtException(JWT_VALIDATE_FIAL, "* AccessToken 검증 중 익셉션 발생. "+e.getMessage(), e);
		}
	}
	
	/** 리프레쉬 토큰 검증
	 * @param token
	 * @return userId
	 * 
	 * @throws TimeOutJwtException {@link #validateRefreshToken} 시간 초과
	 * @throws ValidateJwtException {@link #validateRefreshToken} JWT 파싱 실패
	 */
	public String validateRefreshToken(String token) {
		log.debug("* RefreshToken 검증. RefreshToken: [{}]", LogMaskingUtil.maskToken(token, MaskLevel.MEDIUM));

		try {
			Claims claims = Jwts.parserBuilder()
					.setSigningKey(signRefreshHmacShaKey)
					.build()
					.parseClaimsJws(token)
					.getBody();
			
			String encryptedUserId = claims.getSubject(); 
			return aesProvider.decrypt(encryptedUserId);
		} catch (ExpiredJwtException e) {
			throw new TimeOutJwtException(JWT_EXPIRED, "RefreshToken 시간 만료. "+e.getMessage(), e);
		} catch (Exception e) {
			throw new ValidateJwtException(JWT_VALIDATE_FIAL, "RefreshToken 검증 중 익셉션 발생. "+e.getMessage(), e);
		}
	}
	
	/** 휴대폰,이메일 auth 토큰 검증
	 * @param token
	 * @return 복호화된 유저 정보
	 * 
	 * @throws TimeOutJwtException {@link #validateAuthToken} 시간 초과
	 * @throws ValidateJwtException {@link #validateAuthToken} JWT 파싱 실패
	 */
	public Map<String, Object> validateAuthToken(String token) {
		log.debug("* AuthToken 검증. AuthToken: [{}]", LogMaskingUtil.maskToken(token, MaskLevel.MEDIUM));
		
		try {
			Claims claims = Jwts.parserBuilder()
					.setSigningKey(authHmacShaKey)
					.build()
					.parseClaimsJws(token)
					.getBody();
			
			return getDecryptedClaims(claims);
		} catch (ExpiredJwtException e) {
			throw new TimeOutJwtException(JWT_EXPIRED, "AuthToken 시간 만료. "+e.getMessage(), e);
		} catch (Exception e) {
			throw new ValidateJwtException(JWT_VALIDATE_FIAL, "AuthToken 검증 중 익셉션 발생. "+e.getMessage(), e);
		}
	}
	
	/** AccessToken 남은 시간 조회
	 * @param token
	 * @return 토큰 남은시간
	 *  
	 * @throws TimeOutJwtException {@link #getSignJwtRemainingTime} 시간 초과
	 * @throws ValidateJwtException {@link #getSignJwtRemainingTime} JWT 파싱 실패
	 */
	public long getAccessTokenRemainingTime(String accessToken) {
		log.debug("* AccessToken 토큰 유효시간 검증. Token: [{}]", LogMaskingUtil.maskToken(accessToken, MaskLevel.MEDIUM));

		try {
			Claims claims = Jwts.parserBuilder()
					.setSigningKey(signAccessHmacShaKey)
					.build()
					.parseClaimsJws(accessToken)
					.getBody();
			
			Date expiration = claims.getExpiration();
			return expiration.getTime() - System.currentTimeMillis();
		} catch (ExpiredJwtException e) {
			throw new TimeOutJwtException(JWT_EXPIRED,"이미 만료된 토큰. "+e.getMessage());
		} catch (Exception e) {
			throw new ValidateJwtException(JWT_VALIDATE_FIAL,"토큰 검증 중 익셉션 발생. "+e.getMessage(),e);
		}
	}
	
	/** AuthToken 남은 시간 조회
	 * @param token
	 * @return 토큰 남은시간
	 *  
	 * @throws TimeOutJwtException {@link #getSignJwtRemainingTime} 시간 초과
	 * @throws ValidateJwtException {@link #getSignJwtRemainingTime} JWT 파싱 실패
	 */
	public long getAuthTokenRemainingTime(String authToken) {
		log.debug("* AuthToken 토큰 유효시간 검증. Token: [{}]", LogMaskingUtil.maskToken(authToken, MaskLevel.MEDIUM));

		try {
			Claims claims = Jwts.parserBuilder()
					.setSigningKey(authHmacShaKey)
					.build()
					.parseClaimsJws(authToken)
					.getBody();
			
			Date expiration = claims.getExpiration();
			return expiration.getTime() - System.currentTimeMillis();
		} catch (ExpiredJwtException e) {
			throw new TimeOutJwtException(JWT_EXPIRED,"이미 만료된 토큰. "+e.getMessage());
		} catch (Exception e) {
			throw new ValidateJwtException(JWT_VALIDATE_FIAL,"토큰 검증 중 익셉션 발생. "+e.getMessage(),e);
		}
	}
	
	
	/** Claims 복호화
	 * @param claims
	 * @return Claims
	 * 
     * @throws DecryptException {@link AesProvider#decrypt} 복호화 실패
	 */
	@SuppressWarnings("unused")
	private Map<String,Object> getDecryptedClaims(Claims claims) {
		Map<String, Object> result = new HashMap<>();
		result.put("subject",aesProvider.decrypt(claims.getSubject()));
		claims.forEach((k, v) -> {
			if (v instanceof String strVal) {
				result.put(k, aesProvider.decrypt((String)v));
			} else {
				result.put(k, v);
			}
		});
		return result;
	}
	
}
