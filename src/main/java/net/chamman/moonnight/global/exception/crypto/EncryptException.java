package net.chamman.moonnight.global.exception.crypto;

import net.chamman.moonnight.global.exception.CriticalException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class EncryptException extends CriticalException{

	public EncryptException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public EncryptException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public EncryptException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public EncryptException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}



	
}
