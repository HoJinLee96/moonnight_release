package net.chamman.moonnight.global.exception.infra;

import net.chamman.moonnight.global.exception.CriticalException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class EmailSendException extends CriticalException{

	public EmailSendException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public EmailSendException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public EmailSendException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public EmailSendException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}
	
}
