package net.chamman.moonnight.global.exception.infra.s3;

import net.chamman.moonnight.global.exception.CriticalException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class S3UploadException extends CriticalException{

	public S3UploadException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public S3UploadException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public S3UploadException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public S3UploadException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}
	
	

}
