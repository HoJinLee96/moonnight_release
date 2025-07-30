package net.chamman.moonnight.global.exception.status;

import net.chamman.moonnight.global.exception.HttpStatusCode;

public class StatusDeleteException extends StatusException{

	public StatusDeleteException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public StatusDeleteException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public StatusDeleteException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public StatusDeleteException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}

	
  
}
