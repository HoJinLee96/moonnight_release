package net.chamman.moonnight.global.exception.status;

import net.chamman.moonnight.global.exception.HttpStatusCode;

public class StatusStopException extends StatusException{

	public StatusStopException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public StatusStopException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public StatusStopException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public StatusStopException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}

	
}
