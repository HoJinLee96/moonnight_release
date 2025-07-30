package net.chamman.moonnight.view;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.annotation.ClientSpecific;
import net.chamman.moonnight.global.validator.RedirectResolver;

@Controller
@Slf4j
@RequiredArgsConstructor
public class SignViewController {

	private final RedirectResolver redirectResolver;

	@GetMapping("/signin")
	public String showLogin(HttpServletRequest req, HttpServletResponse res, Model model) {

		String redirect = req.getParameter("redirect");
		if (redirect == null) {
			String referer = req.getHeader("Referer");
			if (referer != null) {
				if (redirectResolver.isAuthorizedRedirectUri(referer)) {
					redirect = Base64.getEncoder().encodeToString(referer.getBytes(StandardCharsets.UTF_8));
				}
			}
		}
		model.addAttribute("redirect", redirect);
		log.debug("*model.addAttribute(\"redirect\": {})", redirect);
		return "signin/signin";
	}

	@GetMapping("/signinBlank")
	public String showLoginBlank() {
		return "signin/signinBlank";
	}

	@GetMapping("/signup1")
	public String showSignUp1() {
		return "signup/signup1";
	}

	@GetMapping("/signup2")
	public String showSignUp2(
			@ClientSpecific(required = false, value = "X-Access-SignUp-Token") String accessSignUpToken) {
		if (accessSignUpToken == null || accessSignUpToken.isBlank()) {
			return "redirect:/signup1";
		}
		return "signup/signup2";
	}

	@GetMapping("/signup/restoration")
	public String showSignUpRstoration(
			@ClientSpecific(required = false, value = "X-Access-SignUp-Token") String accessSignUpToken,
			@ClientSpecific(required = false, value = "X-Access-SignUp-OAuth-Token") String accessSignUpOAuthToken) {
		if (accessSignUpToken==null && accessSignUpOAuthToken==null) {
			return "redirect:/signup1";
		} else {
			return "signup/signupRestoration";
		}
	}

	@GetMapping("/signup/oauth")
	public String showSignupOAuth(@ClientSpecific(value = "X-Access-SignUp-OAuth-Token") String signUpOAuthToken,
			HttpServletRequest req, HttpServletResponse res) {
		return "signup/signupOAuth";
	}

	@GetMapping("/signup/oauth/choice")
	public String showSignupOAuthChoice(@ClientSpecific(value = "X-Access-SignUp-OAuth-Token") String signUpOAuthToken,
			HttpServletRequest req, HttpServletResponse res) {
		return "signup/signupOAuthChoice";
	}

	@GetMapping("/signup/oauth/restoration")
	public String showSignupOAuthRestoration(
			@ClientSpecific(value = "X-Access-SignUp-OAuth-Token") String signUpOAuthToken, HttpServletRequest req,
			HttpServletResponse res) {
		return "signup/signupOAuthChoice";
	}

	@GetMapping("/signup/exist/local")
	public String showSignupExistLocal(
			@ClientSpecific(value = "X-Link-OAuth-Token") String linkOAuthToken, 
			@RequestParam String email, Model model, HttpServletRequest req, HttpServletResponse res) {
		String redirect = req.getParameter("redirect");
		if (redirect == null) {
			String referer = req.getHeader("Referer");
			if (referer != null) {
				if (redirectResolver.isAuthorizedRedirectUri(referer)) {
					redirect = Base64.getEncoder().encodeToString(referer.getBytes(StandardCharsets.UTF_8));
				}
			}
		}
		model.addAttribute("redirect", redirect);
		log.debug("* model.addAttribute(\"redirect\": {})", redirect);
		
		byte[] decodedBytes = Base64.getDecoder().decode(email);
		String decodedEmail = new String(decodedBytes, StandardCharsets.UTF_8);
		model.addAttribute("email", decodedEmail);

		return "signup/signupExistLocal";
	}

	@GetMapping("/signup/exist/oauth")
	public String showJoinSnsConfirm(@RequestParam List<String> providers, Model model) {

		if (providers == null) {
			return "error/400";
		}
		log.debug("* 이미 가입된 소셜 계정 안내 페이지. Providers: {}", providers);
		model.addAttribute("existingProviders", providers);

		return "signup/signupExistOAuth";
	}

	@GetMapping("/sign/stay")
	public String showUserStatusStay(HttpServletRequest req, HttpServletResponse res) {
		return "sign/signStay";
	}

	@GetMapping("/sign/stop")
	public String showUserStatusStop(HttpServletRequest req, HttpServletResponse res) {
		return "sign/signStop";
	}

	@GetMapping("/sign/delete")
	public String showUserStatusDelete(HttpServletRequest req, HttpServletResponse res) {
		return "sign/signDelete";
	}
	
}
