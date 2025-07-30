package net.chamman.moonnight.global.exception.sign;

import lombok.Getter;
import net.chamman.moonnight.auth.oauth.OAuth.OAuthProvider;
import net.chamman.moonnight.global.exception.CustomException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

@Getter
public class AccountMergeRequiredException extends CustomException {
	
	OAuthProvider existOauthProvider;

	public AccountMergeRequiredException(HttpStatusCode httpStatusCode ,OAuthProvider oauthProvider) {
		super(httpStatusCode);
		this.existOauthProvider = oauthProvider;
	}

}
