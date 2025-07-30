package net.chamman.moonnight.global.exception.jwt;

import net.chamman.moonnight.global.exception.CriticalException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class ValidateJwtException extends CriticalException{

	public ValidateJwtException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public ValidateJwtException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public ValidateJwtException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public ValidateJwtException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}

	
 
  
}
