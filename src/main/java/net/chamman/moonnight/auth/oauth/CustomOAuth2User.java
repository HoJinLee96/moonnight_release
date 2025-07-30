package net.chamman.moonnight.auth.oauth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.chamman.moonnight.auth.crypto.AesProvider;
import net.chamman.moonnight.auth.crypto.Encryptable;
import net.chamman.moonnight.auth.oauth.OAuth.OAuthProvider;
import net.chamman.moonnight.auth.token.TokenProvider.TokenType;
import net.chamman.moonnight.global.util.LogMaskingUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil.MaskLevel;

@Getter
@Setter
@NoArgsConstructor
public class CustomOAuth2User implements Encryptable<CustomOAuth2User>{
	private String oauthProviderId;
	private OAuthProvider oauthProvider;
	private String email;
	private String name;
	public static final TokenType TOKENTYPE = TokenType.ACCESS_SIGNUP_OAUTH;


	public CustomOAuth2User(String oauthProviderId, OAuthProvider oauthProvider, String email, String name) {
		this.oauthProviderId = oauthProviderId;
		this.oauthProvider = oauthProvider;
		this.email = email;
		this.name = name;
	}

	public String toString(MaskLevel maskLevel) {
		return "CustomOAuth2User: " + "[oauthProviderId= " + LogMaskingUtil.maskId(oauthProviderId, maskLevel) + ", oauthProvider= "
				+ oauthProvider + ", email= " + LogMaskingUtil.maskEmail(email, maskLevel) + ", name= "
				+ LogMaskingUtil.maskName(name, maskLevel) + "]";
	}

	@Override
	public CustomOAuth2User encrypt(AesProvider aesProvider) {
		return new CustomOAuth2User(
				aesProvider.encrypt(this.oauthProviderId),
				this.oauthProvider,
				aesProvider.encrypt(this.email),
				aesProvider.encrypt(this.name)
				);
	}

	@Override
	public CustomOAuth2User decrypt(AesProvider aesProvider) {
		return new CustomOAuth2User(
				aesProvider.decrypt(this.oauthProviderId),
				this.oauthProvider,
				aesProvider.decrypt(this.email),
				aesProvider.decrypt(this.name)
				);
	}
	
	public OAuth toEntity() {
		return OAuth.builder()
				.oauthProvider(this.oauthProvider)
				.oauthProviderId(this.oauthProviderId)
				.build();
	}
}