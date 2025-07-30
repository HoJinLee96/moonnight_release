package net.chamman.moonnight.global.exception.user;

import lombok.Getter;
import net.chamman.moonnight.domain.user.User.UserProvider;
import net.chamman.moonnight.global.exception.CustomException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

@Getter
public class DuplicationException extends CustomException {

	UserProvider userProvider;
	String existValue;

	public DuplicationException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public DuplicationException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public DuplicationException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public DuplicationException(HttpStatusCode httpStatusCode, UserProvider userProvider, String existValue) {
		super(httpStatusCode);
		this.userProvider = userProvider;
		this.existValue = existValue;
	}

	public DuplicationException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);
	}

}