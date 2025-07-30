package net.chamman.moonnight.global.validator;

import static net.chamman.moonnight.global.exception.HttpStatusCode.ILLEGAL_REQUEST;
import static net.chamman.moonnight.global.exception.HttpStatusCode.TOKEN_NOT_FOUND;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.util.WebUtils; // Cookie 찾는 데 유용

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.annotation.ClientSpecific;
import net.chamman.moonnight.global.context.RequestContextHolder;
import net.chamman.moonnight.global.exception.IllegalRequestException;
import net.chamman.moonnight.global.exception.token.IllegalTokenException;
import net.chamman.moonnight.global.util.LogMaskingUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil.MaskLevel;

@Slf4j
@Component
public class ClientSpecificArgumentResolver implements HandlerMethodArgumentResolver {
	
	// 1. 이 Resolver가 어떤 파라미터를 지원(처리)할 것인가?
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// 파라미터에 @ClientSpecific 어노테이션이 붙어있는지 확인
		return parameter.hasParameterAnnotation(ClientSpecific.class);
	}
	
	// 2. 실제 파라미터 값을 어떻게 만들어낼 것인가? (핵심 로직)
	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		
		// @ClientSpecific("여기에_넣은_값") 가져오기
		ClientSpecific clientSpecificAnnotation = parameter.getParameterAnnotation(ClientSpecific.class);
		String valueName = clientSpecificAnnotation.value();
		boolean isRequired = clientSpecificAnnotation.required();
		
		// HttpServletRequest 객체 가져오기 (헤더, 쿠키 등에 접근하기 위해)
		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		if (request == null) {
			throw new IllegalRequestException(ILLEGAL_REQUEST,"요청 정보를 가져올 수 없습니다.");
		}
		
		boolean isMobileApp = RequestContextHolder.getContext().isMobileApp();
		String clientTypeKr = isMobileApp ? "모바일 앱" : "웹";
		log.debug("*{} 접근. {} 토큰 확인.", clientTypeKr, valueName);

		String token = null;
		
		if (isMobileApp) {
			token = request.getHeader(valueName);
		} else {
			Cookie cookie = WebUtils.getCookie(request, valueName);
			if (cookie != null) {
				token = cookie.getValue();
			}
		}
		
		// 토큰 유효성 검사 (null 또는 빈 값/공백 체크)
		if (token == null || token.isBlank()) {
			log.warn("[{}] {} 요청에 필요한 토큰이 없습니다.", valueName, clientTypeKr);
			if(isRequired) {
				throw new IllegalTokenException(TOKEN_NOT_FOUND,clientTypeKr + "으로 부터 받은 요청에 필요한 '" + valueName + "' 토큰이 없습니다.");
			}else {
				return null;
			}
		}
		
		log.debug("*{} 접근 {} 토큰 확인 완료. Token Value: [{}]", clientTypeKr, valueName, LogMaskingUtil.maskToken(token, MaskLevel.MEDIUM));
		return token;
	}

}