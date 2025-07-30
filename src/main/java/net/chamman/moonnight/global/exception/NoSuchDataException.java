package net.chamman.moonnight.global.exception;

public class NoSuchDataException extends CustomException{

	public NoSuchDataException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public NoSuchDataException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public NoSuchDataException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public NoSuchDataException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}

	
}
