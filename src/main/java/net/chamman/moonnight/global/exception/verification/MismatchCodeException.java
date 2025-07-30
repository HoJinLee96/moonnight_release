package net.chamman.moonnight.global.exception.verification;

import net.chamman.moonnight.global.exception.CustomException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class MismatchCodeException extends CustomException{

	public MismatchCodeException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public MismatchCodeException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public MismatchCodeException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public MismatchCodeException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}
	
	

}
