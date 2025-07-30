package net.chamman.moonnight.auth.token.dto;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.chamman.moonnight.auth.crypto.AesProvider;
import net.chamman.moonnight.auth.crypto.Encryptable;
import net.chamman.moonnight.auth.token.TokenProvider.TokenType;
import net.chamman.moonnight.global.exception.HttpStatusCode;
import net.chamman.moonnight.global.exception.IllegalRequestException;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VerificationPhoneTokenDto implements Encryptable<VerificationPhoneTokenDto>{
	
	String verificationId;
	String phone;
	public static final TokenType TOKENTYPE = TokenType.VERIFICATION_PHONE;
	
	
	@Override
	public VerificationPhoneTokenDto encrypt(AesProvider aesProvider) {
		return new VerificationPhoneTokenDto(aesProvider.encrypt(this.verificationId),aesProvider.encrypt(this.phone));
	}
	
	@Override
	public VerificationPhoneTokenDto decrypt(AesProvider aesProvider) {
		return new VerificationPhoneTokenDto(aesProvider.decrypt(this.verificationId),aesProvider.decrypt(this.phone));
	}
	
	@JsonIgnore
	public int getIntVerificationId() {
		return Integer.parseInt(verificationId);
	}
	
	public void comparePhone(String phone) {
		if(!Objects.equals(this.phone, phone)) {
			throw new IllegalRequestException(HttpStatusCode.ILLEGAL_INPUT_VALUE);
		}
	}
	
}
