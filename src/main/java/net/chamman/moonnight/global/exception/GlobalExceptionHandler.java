package net.chamman.moonnight.global.exception;

import static net.chamman.moonnight.global.exception.HttpStatusCode.INTERNAL_SERVER_ERROR;
import static net.chamman.moonnight.global.exception.HttpStatusCode.REQUEST_BODY_NOT_VALID;

import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.util.ApiResponseDto;
import net.chamman.moonnight.global.util.ApiResponseFactory;

@Slf4j
@Order(1)
@RestControllerAdvice(basePackages = {"net.chamman.moonnight.domain","net.chamman.moonnight.infra","net.chamman.moonnight.auth","net.chamman.moonnight.global"})
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private final ApiResponseFactory apiResponseFactory;

	@ExceptionHandler(CriticalException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleCriticalException(CriticalException ex) {
		HttpStatusCode httpStatusCode = ex.getHttpStatusCode();
		log.error("* {} 발생. HttpStatusCode: [{}]", ex.getClass().getSimpleName(), httpStatusCode.toString(), ex);
		return ResponseEntity.status(httpStatusCode.getStatus()).body(apiResponseFactory.error(httpStatusCode));
	}

	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleCustomException(CustomException ex) {
		HttpStatusCode httpStatusCode = ex.getHttpStatusCode();
		log.debug("* {} 발생. HttpStatusCode: [{}]", ex.getClass().getSimpleName(), httpStatusCode.toString(), ex);
		return ResponseEntity.status(httpStatusCode.getStatus()).body(apiResponseFactory.error(httpStatusCode));
	}

	// @Valid @RequestBody로 받은 **객체(DTO)**를 검증할 때 사용. 실패 시 MethodArgumentNotValidException 발생.
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleMethodArgumentNotValidException(
			MethodArgumentNotValidException ex, HttpServletRequest request) {
		log.debug("* MethodArgumentNotValidException 발생.");

		BindingResult bindingResult = ex.getBindingResult();
		String messageKey = bindingResult.getFieldError().getDefaultMessage();
		
		if (messageKey != null && !messageKey.isEmpty()) {
			try {
				return ResponseEntity.status(400).body(apiResponseFactory.errorWithMessageKey(REQUEST_BODY_NOT_VALID, messageKey));
			} catch (Exception e) {
				log.warn("* 메시지 번역 실패.", e);
				return ResponseEntity.status(400)
						.body(apiResponseFactory.errorWithMessage(REQUEST_BODY_NOT_VALID, "입력 값이 올바르지 않습니다."));
			}
		}

		return ResponseEntity.status(400).body(apiResponseFactory.errorWithMessageKey(REQUEST_BODY_NOT_VALID, "입력 값이 올바르지 않습니다."));
	}

	// @Validated 컨트롤러 클래스에 붙여서 @RequestParam이나 @PathVariable 같은 단일 파라미터를 검증할 때 사용. 실패 시 ConstraintViolationException 발생.
	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleHandlerMethodValidationException(
			HandlerMethodValidationException ex, HttpServletRequest request) {
		log.debug("* HandlerMethodValidationException 발생.");

		// 첫 번째 에러의 메시지 키를 가져옴
		String messageKey = ex.getAllErrors().get(0).getDefaultMessage();

		if (messageKey != null && !messageKey.isEmpty()) {
			try {
				return ResponseEntity.status(400).body(apiResponseFactory.errorWithMessageKey(REQUEST_BODY_NOT_VALID, messageKey));
			} catch (Exception e) {
				log.warn("* 메시지 번역 실패.", e);
				return ResponseEntity.status(400)
						.body(apiResponseFactory.errorWithMessage(REQUEST_BODY_NOT_VALID, "잘못된 입력값 입니다."));
			}
		}
		return ResponseEntity.status(400).body(apiResponseFactory.errorWithMessage(REQUEST_BODY_NOT_VALID, "잘못된 입력값 입니다."));
	}
	
	// @RequestParam, @PathVariable 타입 변환 실패시
	// ConversionService 담당
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleMethodArgumentTypeMismatchException(
	        MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
	    
	    // 어떤 파라미터에서 에러가 났는지 더 친절하게 로그를 남길 수 있어요.
	    log.debug("* MethodArgumentTypeMismatchException 발생: 파라미터 '{}'에 잘못된 값 '{}'이(가) 입력되었습니다.",
	             ex.getName(), ex.getValue());

	    return ResponseEntity.status(400)
	            .body(apiResponseFactory.errorWithMessage(REQUEST_BODY_NOT_VALID,"잘못된 입력값 입니다."));
	}
	
	// @RequestBody 역질렬화 실패시
	// HttpMessageConverter 담당
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleHttpMessageNotReadableException(
	        HttpMessageNotReadableException ex, HttpServletRequest request) {
	    
	    log.debug("* HttpMessageNotReadableException 발생: {}", ex.getMessage());

	    return ResponseEntity.status(400)
	            .body(apiResponseFactory.errorWithMessage(REQUEST_BODY_NOT_VALID,"잘못된 입력값 입니다."));
	}
	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponseDto<Void>> handleAllExceptions(Exception e, HttpServletRequest request) {
		log.error("* 예상치 못한 익셉션 발생.", e);
		return ResponseEntity.status(500).body(apiResponseFactory.error(INTERNAL_SERVER_ERROR));
	}

}
