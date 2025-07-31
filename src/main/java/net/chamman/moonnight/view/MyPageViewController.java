package net.chamman.moonnight.view;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;

@Controller
@EnableMethodSecurity
@RequiredArgsConstructor
public class MyPageViewController {
//	
//	private final EstimateService estimateService; 
//	private final AdminService userService; 
//	private final AesProvider aesProvider; 
//	
//	
//	@GetMapping("/my")
//	public String showMy(@AuthenticationPrincipal CustomAdminDetails userDetails, Model model) {
//		return "my/my";
//	}
//
//	@GetMapping("/my/estimate")
//	public String showMyEstimate(@AuthenticationPrincipal CustomAdminDetails userDetails, Model model) {
//		
//		List<EstimateResponseDto> estimateList = estimateService.getMyAllEstimate(userDetails.getUserId());
//        model.addAttribute("estimateList", estimateList);
//
//		return "my/myEstimate";
//	}
//	
//	@GetMapping("/my/signInfo")
//	public String showMyLoginInfo(
//			@AuthenticationPrincipal CustomAdminDetails userDetails,
//			Model model) {
//		
//		Admin user = userService.getActiveUserByUserId(userDetails.getUserId());
//        model.addAttribute("user", AdminResponseDto.fromEntity(user,null));
//        List<OAuthResponseDto> oauthResponseList = oauthService.getOAuthByUser(user);
//        model.addAttribute("linkOAuths", oauthResponseList);
//        
//		return "my/mySignInfo";
//	}
//	
//    @GetMapping("/my/signInfo/link/{provider}")
//    public String startAccountLink(
//    		@PathVariable String provider, 
//    		@AuthenticationPrincipal CustomAdminDetails userDetails,
//    		HttpServletRequest request) {
//
//    	String userId = userDetails.getUsername();
//        // 1. 세션에 "계정 연동 작업 중"이라는 깃발을 꽂는다.
//        request.getSession().setAttribute("OAUTH_LINK_IN_PROGRESS", true);
//        request.getSession().setAttribute("LINKING_USER_ID", aesProvider.encrypt(userId));
//        
//        // 2. 원래의 소셜 로그인 주소로 리다이렉트
//        return "redirect:/oauth2/authorization/" + provider;
//    }
//	
//	@GetMapping("/my/profile")
//	public String showMyProfile(@AuthenticationPrincipal CustomAdminDetails userDetails, Model model) {
//		
//		Admin user = userService.getActiveUserByUserId(userDetails.getUserId());
//        model.addAttribute("user", AdminResponseDto.fromEntity(user,null));
//        
//		return "my/myProfile";
//	}
//	
//	@GetMapping("/my/signInfo/password")
//	public String showMyPassword(@AuthenticationPrincipal CustomAdminDetails userDetails, Model model) {
//		Admin user = userService.getActiveUserByUserId(userDetails.getUserId());
//        model.addAttribute("user", AdminResponseDto.fromEntity(user,null));
//		return "my/myPassword";
//	}
//	
//	@GetMapping("/my/signInfo/withdrawal")
//	public String showWithdrawal() {
//		return "sign/withdrawal";
//	}
//	
//	@GetMapping("/my/signInfo/convertToLocal")
//	@PreAuthorize("hasRole('OAUTH')")
//	public String showConvertToLocal() {
//		return "sign/convertToLocal";
//	}
	
}
