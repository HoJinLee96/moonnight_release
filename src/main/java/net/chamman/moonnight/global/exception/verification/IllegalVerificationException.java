package net.chamman.moonnight.global.exception.verification;

import net.chamman.moonnight.global.exception.CustomException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class IllegalVerificationException extends CustomException{

	public IllegalVerificationException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public IllegalVerificationException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public IllegalVerificationException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public IllegalVerificationException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}
	
}
