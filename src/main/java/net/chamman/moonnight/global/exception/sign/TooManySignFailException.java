package net.chamman.moonnight.global.exception.sign;

import net.chamman.moonnight.global.exception.CustomException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class TooManySignFailException extends CustomException{

	public TooManySignFailException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public TooManySignFailException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public TooManySignFailException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public TooManySignFailException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}


  
}
