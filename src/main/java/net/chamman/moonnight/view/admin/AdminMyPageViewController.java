package net.chamman.moonnight.view.admin;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.domain.admin.Admin;
import net.chamman.moonnight.domain.admin.AdminService;
import net.chamman.moonnight.domain.admin.dto.AdminResponseDto;
import net.chamman.moonnight.global.security.principal.CustomAdminDetails;

@Controller
@EnableMethodSecurity
@RequiredArgsConstructor
public class AdminMyPageViewController {

	private final AdminService adminService;

	@GetMapping("/my")
	public String showMy(@AuthenticationPrincipal CustomAdminDetails adminDetails, Model model) {
		return "my/my";
	}

	@GetMapping("/my/signInfo")
	public String showMyLoginInfo(@AuthenticationPrincipal CustomAdminDetails adminDetails, Model model) {

		Admin admin = adminService.getActiveAdminByAdminId(adminDetails.getAdminId());
		model.addAttribute("admin", AdminResponseDto.fromEntity(admin));

		return "my/mySignInfo";
	}

	@GetMapping("/my/profile")
	public String showMyProfile(@AuthenticationPrincipal CustomAdminDetails adminDetails, Model model) {

		Admin admin = adminService.getActiveAdminByAdminId(adminDetails.getAdminId());
		model.addAttribute("admin", AdminResponseDto.fromEntity(admin));

		return "my/myProfile";
	}

	@GetMapping("/my/signInfo/password")
	public String showMyPassword(@AuthenticationPrincipal CustomAdminDetails adminDetails, Model model) {
		Admin admin = adminService.getActiveAdminByAdminId(adminDetails.getAdminId());
		model.addAttribute("admin", AdminResponseDto.fromEntity(admin));
		return "my/myPassword";
	}

	@GetMapping("/my/signInfo/withdrawal")
	public String showWithdrawal() {
		return "my/withdrawal";
	}
}
