package net.chamman.moonnight.global.exception.infra;

import net.chamman.moonnight.global.exception.CustomException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class InvalidMainAddressException extends CustomException{

	public InvalidMainAddressException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public InvalidMainAddressException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public InvalidMainAddressException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public InvalidMainAddressException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}

	
}
