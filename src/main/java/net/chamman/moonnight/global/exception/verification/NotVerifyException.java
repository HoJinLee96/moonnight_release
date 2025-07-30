package net.chamman.moonnight.global.exception.verification;

import net.chamman.moonnight.global.exception.CustomException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class NotVerifyException extends CustomException{

	public NotVerifyException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public NotVerifyException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public NotVerifyException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public NotVerifyException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}

	
}
