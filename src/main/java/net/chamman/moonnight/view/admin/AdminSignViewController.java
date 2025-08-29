package net.chamman.moonnight.view.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.annotation.ClientSpecific;

@Controller
@Slf4j
@RequiredArgsConstructor
public class AdminSignViewController {

	@GetMapping("/admin/signIn")
	public String showLogin(@ClientSpecific(required = false, value = "X-Access-Token") String accessToken, HttpServletRequest req,
			HttpServletResponse res, Model model) {
		
		log.debug("* accessToken: [{}]",accessToken);
		if (accessToken != null) {
			return "redirect:/admin/home";
		}
		return "admin/sign/signIn";
	}

	@GetMapping("/admin/signUp1")
	public String showSignUp1() {
		return "admin/sign/signUp1";
	}

	@GetMapping("/admin/signUp2")
	public String showSignUp2(
			@ClientSpecific(required = false, value = "X-Access-SignUp-Token") String accessSignUpToken) {
		if (accessSignUpToken == null || accessSignUpToken.isBlank()) {
			return "redirect:/signup1";
		}
		return "admin/sign/signUp2";
	}

	@GetMapping("/admin/sign/stay")
	public String showAdminStatusStay(HttpServletRequest req, HttpServletResponse res) {
		return "admin/sign/signStay";
	}

	@GetMapping("/admin/sign/stop")
	public String showAdminStatusStop(HttpServletRequest req, HttpServletResponse res) {
		return "admin/sign/signStop";
	}

	@GetMapping("/admin/sign/delete")
	public String showAdminStatusDelete(HttpServletRequest req, HttpServletResponse res) {
		return "admin/sign/signDelete";
	}

	@GetMapping("/admin/find/email")
	public String showAdminFindEmail(HttpServletRequest req, HttpServletResponse res) {
		return "admin/find/email";
	}

	@GetMapping("/admin/find/password")
	public String showAdminFindPassword(HttpServletRequest req, HttpServletResponse res) {
		return "admin/find/password";
	}

}
