package net.chamman.moonnight.global.util;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.exception.HttpStatusCode;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiResponseFactory{
	
	private final MessageSource messageSource;
	
	// 성공 응답 (데이터 포함)
    public <T> ApiResponseDto<T> success(HttpStatusCode httpStatusCode, T data) {
        String message = getMessage(httpStatusCode.getMessageKey());
        return ApiResponseDto.of(httpStatusCode, message, data);
    }

    // 성공 응답 (데이터 없음)
    public ApiResponseDto<Void> success(HttpStatusCode httpStatusCode) {
        String message = getMessage(httpStatusCode.getMessageKey());
        return ApiResponseDto.of(httpStatusCode, message, null);
    }

    // 실패 응답
    public ApiResponseDto<Void> error(HttpStatusCode httpStatusCode) {
        String message = getMessage(httpStatusCode.getMessageKey());
        return ApiResponseDto.of(httpStatusCode, message, null);
    }
    
//     실패 응답
    public ApiResponseDto<Void> errorWithMessageKey(HttpStatusCode httpStatusCode, String messageKey) {
        String message = getMessage(messageKey);
        return ApiResponseDto.of(httpStatusCode, message, null);
    }
    
//  실패 응답
	public ApiResponseDto<Void> errorWithMessage(HttpStatusCode httpStatusCode, String message) {
     return ApiResponseDto.of(httpStatusCode, message, null);
 }
    
    // 실패 응답
    public <T> ApiResponseDto<T> error(HttpStatusCode httpStatusCode, T data) {
        String message = getMessage(httpStatusCode.getMessageKey());
        return ApiResponseDto.of(httpStatusCode, message, data);
    }
    
    // 메시지 키로 번역된 메시지를 가져오는 헬퍼 메서드
    private String getMessage(String messageKey) {
    	try {
    		if (messageKey == null || messageKey.isEmpty()) {
    			return "";
    		}
    		// 현재 요청의 Locale 정보를 가져옴 (ko-KR, en-US 등)
    		Locale locale = LocaleContextHolder.getLocale();
    		return messageSource.getMessage(messageKey, null, locale);
    	} catch (Exception e) {
			log.warn("* MessageSource getMessage 실패.",e);
    		return "";
		}
    }
}
