package net.chamman.moonnight.global.exception.infra;

import net.chamman.moonnight.global.exception.CriticalException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class SmsSendException extends CriticalException{

	public SmsSendException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public SmsSendException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public SmsSendException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public SmsSendException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}

}
