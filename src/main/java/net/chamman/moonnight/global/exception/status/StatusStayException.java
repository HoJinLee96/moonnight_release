package net.chamman.moonnight.global.exception.status;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import net.chamman.moonnight.global.exception.HttpStatusCode;

// DB Status == STAY
public class StatusStayException extends StatusException{

	public StatusStayException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public StatusStayException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public StatusStayException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public StatusStayException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}

	
  
}
