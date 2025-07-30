package net.chamman.moonnight.global.exception.token;

import net.chamman.moonnight.global.exception.CustomException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class TokenValueMismatchException extends CustomException{

	public TokenValueMismatchException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public TokenValueMismatchException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public TokenValueMismatchException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public TokenValueMismatchException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}
	
	

}
