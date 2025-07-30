package net.chamman.moonnight.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class VerificationViewController {
	
	@GetMapping("/verify/phone/blank")
	public String showVerifyPhoneBlank(HttpServletRequest req, HttpServletResponse res) {
		return "verifyPhoneBlank";
	}
	
	@GetMapping("/verify/email/blank")
	public String showVerifyEmailBlank(HttpServletRequest req, HttpServletResponse res) {
		return "verifyEmailBlank";
	}
	
}


