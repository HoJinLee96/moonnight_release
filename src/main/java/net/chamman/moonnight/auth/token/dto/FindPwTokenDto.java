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
public class FindPwTokenDto implements Encryptable<FindPwTokenDto>{
	
	private String userId;
	private String email;
	
	public static final TokenType TOKENTYPE = TokenType.ACCESS_FINDPW;

	@Override
	public FindPwTokenDto encrypt(AesProvider aesProvider) {
		return new FindPwTokenDto(aesProvider.encrypt(userId),aesProvider.encrypt(email));
	}

	@Override
	public FindPwTokenDto decrypt(AesProvider aesProvider) {
		return new FindPwTokenDto(aesProvider.decrypt(userId),aesProvider.decrypt(email));
	}

	@JsonIgnore
	public int getIntUserId() {
		return Integer.parseInt(userId);
	}
	
	public void compareEmail(String email) {
		if(!Objects.equals(this.email, email)) {
			throw new IllegalRequestException(HttpStatusCode.ILLEGAL_INPUT_VALUE);
		}
	}
}
