package net.chamman.moonnight.global.exception;

public abstract class CriticalException extends CustomException{

	public CriticalException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);
	}

	public CriticalException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);
	}

	public CriticalException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);
	}

	public CriticalException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);
	}

	
}
