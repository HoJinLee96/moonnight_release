package net.chamman.moonnight.global.exception.transaction;

import net.chamman.moonnight.global.exception.CustomException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class EncodingException extends CustomException{

	public EncodingException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public EncodingException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public EncodingException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public EncodingException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}
	
	

}
