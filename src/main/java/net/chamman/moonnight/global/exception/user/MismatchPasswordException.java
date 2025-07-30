package net.chamman.moonnight.global.exception.user;

import net.chamman.moonnight.global.exception.CustomException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class MismatchPasswordException extends CustomException{

	public MismatchPasswordException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public MismatchPasswordException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public MismatchPasswordException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public MismatchPasswordException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}
	
	

}
