package net.chamman.moonnight.auth.sign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.auth.oauth.OAuth.OAuthProvider;

public record UnlinkOAuthRequestDto(
		
		@NotBlank(message = "validation.user.password.required")
		@Pattern( regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^\\w\\s])[^\\s]{8,60}$", message = "validation.user.password.invalid")
		@Size(min = 8, max = 30, message = "validation.user.password.length")
		String password,
		String oauthProvider,
		String oauthId
		) {

	public int getOAuthId(Obfuscator obfuscator) {
		return obfuscator.decode(Integer.parseInt(oauthId));
	}
	public OAuthProvider getOAuthProvider() {
		return OAuthProvider.valueOf(oauthProvider);
	}
}
