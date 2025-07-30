package net.chamman.moonnight.global.exception;

import lombok.Getter;

@SuppressWarnings("serial")
@Getter
public abstract class CustomException extends RuntimeException{
  
    private final HttpStatusCode httpStatusCode;

    public CustomException(HttpStatusCode httpStatusCode, String message, Exception e) {
      super(message, e);
      this.httpStatusCode = httpStatusCode;
    }
    
    public CustomException(HttpStatusCode httpStatusCode, String message) {
      super(message);
      this.httpStatusCode = httpStatusCode;
    }
    
    public CustomException(HttpStatusCode httpStatusCode, Exception e) {
      super(e);
      this.httpStatusCode = httpStatusCode;
    }

    public CustomException(HttpStatusCode httpStatusCode) {
        super();
        this.httpStatusCode = httpStatusCode;
    }
    
}
