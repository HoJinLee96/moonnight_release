package net.chamman.moonnight.global.exception.token;

import net.chamman.moonnight.global.exception.CustomException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class IllegalTokenException extends CustomException{

	public IllegalTokenException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public IllegalTokenException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public IllegalTokenException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public IllegalTokenException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}

	
	
}
