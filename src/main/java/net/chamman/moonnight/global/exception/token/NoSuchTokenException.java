package net.chamman.moonnight.global.exception.token;

import net.chamman.moonnight.global.exception.CustomException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class NoSuchTokenException extends CustomException{

	public NoSuchTokenException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public NoSuchTokenException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public NoSuchTokenException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public NoSuchTokenException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}

	
	
}
