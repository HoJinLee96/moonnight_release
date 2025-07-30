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
public class VerificationEmailTokenDto implements Encryptable<VerificationEmailTokenDto>{
	
	String verificationId;
	String email;
	public static final TokenType TOKENTYPE = TokenType.VERIFICATION_EMAIL;
	
	@Override
	public VerificationEmailTokenDto encrypt(AesProvider aesProvider) {
		return new VerificationEmailTokenDto(aesProvider.encrypt(this.verificationId),aesProvider.encrypt(this.email));
	}

	@Override
	public VerificationEmailTokenDto decrypt(AesProvider aesProvider) {
		return new VerificationEmailTokenDto(aesProvider.decrypt(this.verificationId),aesProvider.decrypt(this.email));
	}

	@JsonIgnore
	public int getIntVerificationId() {
		return Integer.parseInt(this.verificationId);
	}
	
	public void compareEmail(String email) {
		if(!Objects.equals(this.email, email)) {
			throw new IllegalRequestException(HttpStatusCode.ILLEGAL_INPUT_VALUE);
		}
	}
	
}
