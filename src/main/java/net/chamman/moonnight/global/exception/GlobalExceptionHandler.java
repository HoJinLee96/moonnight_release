package net.chamman.moonnight.global.exception;

import static net.chamman.moonnight.global.exception.HttpStatusCode.INTERNAL_SERVER_ERROR;
import static net.chamman.moonnight.global.exception.HttpStatusCode.REQUEST_BODY_NOT_VALID;

import java.util.Map;

import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.exception.user.DuplicationException;
import net.chamman.moonnight.global.util.ApiResponseDto;
import net.chamman.moonnight.global.util.ApiResponseFactory;

@Slf4j
@Order(1)
@RestControllerAdvice(basePackages = {"net.chamman.moonnight.domain","net.chamman.moonnight.infra","net.chamman.moonnight.auth","net.chamman.moonnight.global"})
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private final ApiResponseFactory apiResponseFactory;

	@ExceptionHandler(DuplicationException.class)
	public ResponseEntity<ApiResponseDto<Map<String, String>>> handleCriticalException(DuplicationException ex) {
		HttpStatusCode httpStatusCode = ex.getHttpStatusCode();
		log.error("* {} 발생. HttpStatusCode: [{}]", ex.getClass().getSimpleName(), httpStatusCode.toString());
		return ResponseEntity.status(httpStatusCode.getStatus()).body(apiResponseFactory.error(httpStatusCode, Map.of("provider", ex.getUserProvider().toString(), "email", ex.getExistValue())));
	}
	
	@ExceptionHandler(CriticalException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleCriticalException(CriticalException ex) {
		HttpStatusCode httpStatusCode = ex.getHttpStatusCode();
		log.error("* {} 발생. HttpStatusCode: [{}]", ex.getClass().getSimpleName(), httpStatusCode.toString(), ex);
		return ResponseEntity.status(httpStatusCode.getStatus()).body(apiResponseFactory.error(httpStatusCode));
	}

	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleCustomException(CustomException ex) {
		HttpStatusCode httpStatusCode = ex.getHttpStatusCode();
		log.info("* {} 발생. HttpStatusCode: [{}]", ex.getClass().getSimpleName(), httpStatusCode.toString(), ex);
		return ResponseEntity.status(httpStatusCode.getStatus()).body(apiResponseFactory.error(httpStatusCode));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleMethodArgumentNotValidException(
			MethodArgumentNotValidException ex, HttpServletRequest request) {
		log.info("* MethodArgumentNotValidException 발생.");

		BindingResult bindingResult = ex.getBindingResult();
		String messageKey = bindingResult.getFieldError().getDefaultMessage();

		return ResponseEntity.status(400).body(apiResponseFactory.errorWithMessageKey(REQUEST_BODY_NOT_VALID, messageKey));
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleHandlerMethodValidationException(
			HandlerMethodValidationException ex, HttpServletRequest request) {
		log.info("* HandlerMethodValidationException 발생.");
		log.info(ex.getMessage());

		// 첫 번째 에러의 메시지 키를 가져옴
		String messageKey = ex.getAllErrors().get(0).getDefaultMessage();

		if (messageKey != null && !messageKey.isEmpty()) {
			try {
				return ResponseEntity.status(400).body(apiResponseFactory.errorWithMessageKey(REQUEST_BODY_NOT_VALID, messageKey));
			} catch (Exception e) {
				log.warn("* 메시지 번역 실패.", e);
				return ResponseEntity.status(400)
						.body(apiResponseFactory.errorWithMessage(REQUEST_BODY_NOT_VALID, "입력 값에 문제가 있습니다."));
			}
		}
		return ResponseEntity.status(400).body(apiResponseFactory.errorWithMessage(REQUEST_BODY_NOT_VALID, "입력 값에 문제가 있습니다."));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponseDto<Void>> handleAllExceptions(Exception e, HttpServletRequest request) {
		log.error("* 예상치 못한 익셉션 발생.", e);
		return ResponseEntity.status(500).body(apiResponseFactory.error(INTERNAL_SERVER_ERROR));
	}

}
