package net.chamman.moonnight.global.exception.infra.s3;

import net.chamman.moonnight.global.exception.CriticalException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class S3DeleteException extends CriticalException{

	public S3DeleteException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public S3DeleteException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public S3DeleteException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public S3DeleteException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}
	
	
	
	
}
