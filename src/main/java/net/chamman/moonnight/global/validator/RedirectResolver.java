package net.chamman.moonnight.global.validator;

import static net.chamman.moonnight.global.exception.HttpStatusCode.ILLEGAL_INPUT_VALUE;

import java.net.URI;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.annotation.Redirect;
import net.chamman.moonnight.global.context.RequestContextHolder;
import net.chamman.moonnight.global.exception.IllegalRequestException;

@Component
@Slf4j
public class RedirectResolver implements HandlerMethodArgumentResolver {
	
	private final String authorizedRedirectUris;
	
	public RedirectResolver(@Value("${spring.application.domain}") String authorizedRedirectUris) {
		this.authorizedRedirectUris = authorizedRedirectUris;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(Redirect.class);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		
		Redirect clientSpecificAnnotation = parameter.getParameterAnnotation(Redirect.class);
		boolean isRequired = clientSpecificAnnotation.required();

		boolean isMobileApp = RequestContextHolder.getContext().isMobileApp();
		String clientTypeKr = isMobileApp ? "모바일 앱" : "웹";

		String redirect = "/";

		// 모바일 앱이면 바로 리턴
		if (isMobileApp) {
		    log.debug("* [{} 접근] - 리다이렉트 기본값 사용", clientTypeKr);
		    return redirect;
		}

		// redirect 파라미터 처리
		String encodedRedirect = webRequest.getParameter("redirect");
		if (encodedRedirect == null || encodedRedirect.isBlank()) {
		    if (isRequired) {
		        throw new IllegalRequestException(ILLEGAL_INPUT_VALUE, "리다이렉트 파라미터가 필수이지만 값이 비어있음.");
		    }
		    log.debug("* [{} 접근] 리다이렉트 파라미터 없음 - 기본값 사용", clientTypeKr);
		    return redirect;
		}

		// 디코딩 및 검증
		String decodedRedirect;
		try {
		    decodedRedirect = new String(Base64.getUrlDecoder().decode(encodedRedirect));
		} catch (IllegalArgumentException e) {
		    throw new IllegalRequestException(ILLEGAL_INPUT_VALUE, "리다이렉트 파라미터 디코딩 실패");
		}

		// 도메인 검증
		if (!isAuthorizedRedirectUri(decodedRedirect)) {
		    if (isRequired) {
		        throw new IllegalRequestException(ILLEGAL_INPUT_VALUE, "리다이렉트 파라미터가 필수이지만 허용되지 않는 리다이렉트 URI 도메인: ["+decodedRedirect+"]");
		    }
		    log.warn("️* 허용되지 않는 리다이렉트 URI 도메인: [{}]", decodedRedirect);
		    return redirect;
		}

		// 최종 정상 리다이렉트
		log.debug("* [{} 접근] 리다이렉트 URI 정상 - redirect: [{}]", clientTypeKr, decodedRedirect);
		return decodedRedirect;
	}
	
	public boolean isAuthorizedRedirectUri(String uri) {
	    try {
	    	URI clientRedirectUri = new URI(uri);
	        URI authorizedURI = new URI(authorizedRedirectUris);
	        if (authorizedURI.getHost().equalsIgnoreCase(clientRedirectUri.getHost())) { // && authorizedURI.getPort() == clientRedirectUri.getPort()
	        	return true;
	        }
	        return false;
	    } catch (Exception e) {
	        log.warn("* 리다이렉트 URI 도메인 검증 중 익셉션 발생. Redirect URI: {}", uri, e);
	        return false;
	    }
	}

}
