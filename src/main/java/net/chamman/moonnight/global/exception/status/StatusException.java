package net.chamman.moonnight.global.exception.status;

import net.chamman.moonnight.global.exception.CustomException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class StatusException extends CustomException {

	public StatusException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);
	}

	public StatusException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);
	}

	public StatusException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);
	}

	public StatusException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);
	}

}
