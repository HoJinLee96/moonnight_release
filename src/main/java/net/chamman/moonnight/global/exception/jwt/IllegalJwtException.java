package net.chamman.moonnight.global.exception.jwt;

import net.chamman.moonnight.global.exception.CustomException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class IllegalJwtException extends CustomException{

	public IllegalJwtException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public IllegalJwtException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public IllegalJwtException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public IllegalJwtException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}

	
  
}
