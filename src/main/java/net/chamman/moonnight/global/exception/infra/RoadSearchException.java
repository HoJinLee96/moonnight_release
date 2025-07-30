package net.chamman.moonnight.global.exception.infra;

import net.chamman.moonnight.global.exception.CriticalException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class RoadSearchException extends CriticalException{

	public RoadSearchException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public RoadSearchException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public RoadSearchException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public RoadSearchException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}
	
	

}
