package net.chamman.moonnight.global.exception;

public class VersionMismatchException extends CustomException {

	public VersionMismatchException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);
	}

	public VersionMismatchException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);
	}

	public VersionMismatchException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);
	}

	public VersionMismatchException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);
	}
}
