package net.chamman.moonnight.global.interceptor;

import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.chamman.moonnight.global.context.CustomRequestContextHolder;
import net.chamman.moonnight.global.util.ClientIpExtractor;

@Component
public class ClientInfoInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		String clientIp = ClientIpExtractor.extractClientIp(request);
		String clientType = request.getHeader("X-Client-Type");
		boolean isMobileApp = Objects.equals(clientType, "mobile"); 

		CustomRequestContextHolder.setClientIp(clientIp);
		CustomRequestContextHolder.setMobileApp(isMobileApp);

		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception ex) {
		CustomRequestContextHolder.clear(); // 꼭 해줘야 메모리 누수 방지됨
	}

}
