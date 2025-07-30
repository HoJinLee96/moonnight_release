package net.chamman.moonnight.auth.crypto;

import java.math.BigInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:application.properties")
public class Obfuscator {
	
	@Value("${obfuscator.salt}")
	private int salt;       // XOR 키
//	private int MULTIPLIER = 8713;   // 난독화용 곱셈 상수
//	private int INVERSE = 336891841; // MULTIPLIER의 모듈러 역원 (MOD 기준)
//	private int MOD = (1 << 31) - 1; // int 최대값에 가까운 소수
	
	private static final int MULTIPLIER = 8713;
	private static final int MOD = (1 << 31) - 1;
	private static final int INVERSE = BigInteger.valueOf(MULTIPLIER)
	        .modInverse(BigInteger.valueOf(MOD))
	        .intValue(); // ← 이걸로 진짜 값 구해

	
	public int encode(int id) {
		long mixed = ((long) id ^ salt) * MULTIPLIER % MOD;
		return (int) mixed;
	}
	
	public int decode(int obfuscatedId) {
		long unmixed = ((long) obfuscatedId * INVERSE) % MOD;
		return (int) (unmixed ^ salt);
	}
	
	
}
