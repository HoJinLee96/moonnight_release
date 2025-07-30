package net.chamman.moonnight.global.exception.redis;

import net.chamman.moonnight.global.exception.CriticalException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class RedisSetException extends CriticalException{

	public RedisSetException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public RedisSetException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public RedisSetException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public RedisSetException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}
	
	
	
	

}
