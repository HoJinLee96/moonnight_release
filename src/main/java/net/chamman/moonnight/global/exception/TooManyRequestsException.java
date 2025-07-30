package net.chamman.moonnight.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class TooManyRequestsException extends CustomException{

	public TooManyRequestsException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public TooManyRequestsException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public TooManyRequestsException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public TooManyRequestsException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}

	
}
