package net.chamman.moonnight.global.exception.verification;

import net.chamman.moonnight.global.exception.CustomException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class VerificationExpiredException extends CustomException{

	public VerificationExpiredException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public VerificationExpiredException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public VerificationExpiredException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public VerificationExpiredException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}
  
}

