package net.chamman.moonnight.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MainViewController {

	@GetMapping("/swagger-ui/")
	public String redirectToSwagger() {
		return "forward:/swagger-ui/index.html";
	}

//	@GetMapping("/error/404")
//	public String showError() {
//		return "error/404";
//	}

	@GetMapping({ "/", "/home" })
	public String showHome() {
		return "home";
	}

	@GetMapping("/review")
	public String showReview(HttpServletRequest req, HttpServletResponse res) {
		return "review";
	}

	@GetMapping("/find/email")
	public String showFindEmail(HttpServletRequest req, HttpServletResponse res) {
		return "find/findEmail";
	}

	@GetMapping("/find/password")
	public String showFindPassword(HttpServletRequest req, HttpServletResponse res) {
		return "find/findPassword";
	}

	@GetMapping("/update/password/blank")
	public String showUpdatePasswordBlank(HttpServletRequest req, HttpServletResponse res) {
		return "updatePasswordBlank";
	}

}
