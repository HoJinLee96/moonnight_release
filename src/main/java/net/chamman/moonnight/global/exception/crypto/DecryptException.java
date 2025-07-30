package net.chamman.moonnight.global.exception.crypto;

import net.chamman.moonnight.global.exception.CriticalException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class DecryptException extends CriticalException{

	public DecryptException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public DecryptException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public DecryptException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public DecryptException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}

}
