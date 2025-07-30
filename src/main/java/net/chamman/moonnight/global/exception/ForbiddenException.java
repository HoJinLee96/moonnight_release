package net.chamman.moonnight.global.exception;

// DB 생성자와 요청 조회자가 다른 경우
// AccessDenineException 유사 개념
public class ForbiddenException extends CustomException{

	public ForbiddenException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public ForbiddenException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public ForbiddenException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public ForbiddenException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}
	
	
	
}


