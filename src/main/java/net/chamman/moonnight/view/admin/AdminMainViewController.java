package net.chamman.moonnight.view.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class AdminMainViewController {

	@GetMapping("/admin/home")
	public String showHome() {
		return "admin/home";
	}
	
}
