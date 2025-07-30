package net.chamman.moonnight.global.exception.jwt;

import net.chamman.moonnight.global.exception.CustomException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class TimeOutJwtException extends CustomException{

	public TimeOutJwtException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public TimeOutJwtException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public TimeOutJwtException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public TimeOutJwtException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}

	
	

	
}
