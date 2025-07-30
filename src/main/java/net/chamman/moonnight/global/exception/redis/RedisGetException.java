package net.chamman.moonnight.global.exception.redis;

import net.chamman.moonnight.global.exception.CriticalException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class RedisGetException extends CriticalException{

	public RedisGetException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public RedisGetException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public RedisGetException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public RedisGetException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}
	
	

}
