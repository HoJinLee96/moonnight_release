package net.chamman.moonnight.global.exception.transaction;

import net.chamman.moonnight.global.exception.CustomException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class DecodingException extends CustomException{

	public DecodingException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public DecodingException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public DecodingException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public DecodingException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}
	
	
}
