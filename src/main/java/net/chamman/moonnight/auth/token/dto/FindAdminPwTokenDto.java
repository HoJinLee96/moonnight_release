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
public class FindAdminPwTokenDto implements Encryptable<FindAdminPwTokenDto>{
	
	private String adminId;
	private String email;
	
	public static final TokenType TOKENTYPE = TokenType.ACCESS_FINDPW;

	@Override
	public FindAdminPwTokenDto encrypt(AesProvider aesProvider) {
		return new FindAdminPwTokenDto(aesProvider.encrypt(adminId),aesProvider.encrypt(email));
	}

	@Override
	public FindAdminPwTokenDto decrypt(AesProvider aesProvider) {
		return new FindAdminPwTokenDto(aesProvider.decrypt(adminId),aesProvider.decrypt(email));
	}

	@JsonIgnore
	public int getIntAdminId() {
		return Integer.parseInt(adminId);
	}
	
	public void compareEmail(String email) {
		if(!Objects.equals(this.email, email)) {
			throw new IllegalRequestException(HttpStatusCode.ILLEGAL_INPUT_VALUE);
		}
	}
}
