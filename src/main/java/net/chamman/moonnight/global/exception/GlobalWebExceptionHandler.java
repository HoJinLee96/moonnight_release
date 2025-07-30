package net.chamman.moonnight.global.exception;

import org.springframework.core.annotation.Order;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.exception.token.IllegalTokenException;

@Slf4j
@Order(0)
@ControllerAdvice("net.chamman.moonnight.view")
@RequiredArgsConstructor
public class GlobalWebExceptionHandler {

	@ExceptionHandler(IllegalTokenException.class)
	public String handle404(IllegalTokenException ex, HttpServletRequest req, Model model, HttpServletResponse res) {
		
		res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		
		return "error/400";
	}
	
	@ExceptionHandler(NoResourceFoundException.class)
	public String handle404(NoResourceFoundException ex, HttpServletRequest req, Model model, HttpServletResponse res) {
		log.debug("* 404 Not Found: 없는 페이지를 요청했습니다. URL: {}", ex.getResourcePath());

		res.setStatus(HttpServletResponse.SC_NOT_FOUND);

		return "error/404";
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public String handle400(MethodArgumentNotValidException ex, HttpServletRequest req, Model model, HttpServletResponse res) {
		
		res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		
		return "error/400";
	}

}
