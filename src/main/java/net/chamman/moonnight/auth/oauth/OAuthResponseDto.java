package net.chamman.moonnight.auth.oauth;

import lombok.Builder;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.auth.oauth.OAuth.OAuthProvider;
import net.chamman.moonnight.auth.oauth.OAuth.OAuthStatus;

@Builder
public record OAuthResponseDto(
		
		String oauthId,
		OAuthProvider oauthProvider,
		OAuthStatus oauthStatus
		
		
		) {
	
	public static OAuthResponseDto fromEntity(OAuth oauth, Obfuscator obfuscator) {
		return OAuthResponseDto.builder()
				.oauthId(obfuscator.encode(oauth.getOauthId())+"")
				.oauthProvider(oauth.getOauthProvider())
				.oauthStatus(oauth.getOauthStatus())
				.build();
	}

}
