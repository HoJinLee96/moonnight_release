package net.chamman.moonnight.auth.token.dto;

import java.util.Objects;

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
public class SignUpTokenDto implements Encryptable<SignUpTokenDto>{
	
	private String email;
	private String rawPassword;
	public static final TokenType TOKENTYPE = TokenType.ACCESS_SIGNUP;

	@Override
	public SignUpTokenDto encrypt(AesProvider aesProvider) {
        return new SignUpTokenDto(
        		aesProvider.encrypt(this.email), 
                aesProvider.encrypt(this.rawPassword)
        );
	}

	@Override
	public SignUpTokenDto decrypt(AesProvider aesProvider) {
        return new SignUpTokenDto(
        		aesProvider.decrypt(this.email), 
                aesProvider.decrypt(this.rawPassword)
        );
	}
	
	public void compareEmail(String email) {
		if(!Objects.equals(this.email, email)) {
			throw new IllegalRequestException(HttpStatusCode.ILLEGAL_INPUT_VALUE);
		}
	}
	
}
