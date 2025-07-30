package net.chamman.moonnight.view;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.auth.crypto.AesProvider;
import net.chamman.moonnight.auth.oauth.OAuthResponseDto;
import net.chamman.moonnight.auth.oauth.OAuthService;
import net.chamman.moonnight.domain.address.AddressService;
import net.chamman.moonnight.domain.address.dto.AddressResponseDto;
import net.chamman.moonnight.domain.estimate.EstimateService;
import net.chamman.moonnight.domain.estimate.dto.EstimateResponseDto;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.domain.user.UserService;
import net.chamman.moonnight.domain.user.dto.UserResponseDto;
import net.chamman.moonnight.global.security.principal.CustomUserDetails;

@Controller
@EnableMethodSecurity
@RequiredArgsConstructor
public class MyPageViewController {
	
	private final AddressService addressService;
	private final EstimateService estimateService; 
	private final UserService userService; 
	private final OAuthService oauthService; 
	private final AesProvider aesProvider; 
	
	
	@GetMapping("/my")
	public String showMy(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
		return "my/my";
	}
	
    @GetMapping("/my/addressBook")
    public String showMyAddressBook(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {

        List<AddressResponseDto> addressList = addressService.getAddressList(userDetails.getUserId());
        model.addAttribute("addressList", addressList);

        return "my/myAddressBook";
    }
	
	@GetMapping("/my/addressBook/Blank")
	public String showMyAddressBookBlank(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
		
        List<AddressResponseDto> addressList = addressService.getAddressList(userDetails.getUserId());
        model.addAttribute("addressList", addressList);
        
		return "my/myAddressBookBlank";
	}
	
	@GetMapping("/my/estimate")
	public String showMyEstimate(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
		
		List<EstimateResponseDto> estimateList = estimateService.getMyAllEstimate(userDetails.getUserId());
        model.addAttribute("estimateList", estimateList);

		return "my/myEstimate";
	}
	
	@GetMapping("/my/signInfo")
	public String showMyLoginInfo(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			Model model) {
		
		User user = userService.getActiveUserByUserId(userDetails.getUserId());
        model.addAttribute("user", UserResponseDto.fromEntity(user,null));
        List<OAuthResponseDto> oauthResponseList = oauthService.getOAuthByUser(user);
        model.addAttribute("linkOAuths", oauthResponseList);
        
		return "my/mySignInfo";
	}
	
    @GetMapping("/my/signInfo/link/{provider}")
    public String startAccountLink(
    		@PathVariable String provider, 
    		@AuthenticationPrincipal CustomUserDetails userDetails,
    		HttpServletRequest request) {

    	String userId = userDetails.getUsername();
        // 1. 세션에 "계정 연동 작업 중"이라는 깃발을 꽂는다.
        request.getSession().setAttribute("OAUTH_LINK_IN_PROGRESS", true);
        request.getSession().setAttribute("LINKING_USER_ID", aesProvider.encrypt(userId));
        
        // 2. 원래의 소셜 로그인 주소로 리다이렉트
        return "redirect:/oauth2/authorization/" + provider;
    }
	
	@GetMapping("/my/profile")
	public String showMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
		
		User user = userService.getActiveUserByUserId(userDetails.getUserId());
        model.addAttribute("user", UserResponseDto.fromEntity(user,null));
        
		return "my/myProfile";
	}
	
	@GetMapping("/my/signInfo/password")
	public String showMyPassword(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
		User user = userService.getActiveUserByUserId(userDetails.getUserId());
        model.addAttribute("user", UserResponseDto.fromEntity(user,null));
		return "my/myPassword";
	}
	
	@GetMapping("/my/signInfo/withdrawal")
	public String showWithdrawal() {
		return "sign/withdrawal";
	}
	
	@GetMapping("/my/signInfo/convertToLocal")
	@PreAuthorize("hasRole('OAUTH')")
	public String showConvertToLocal() {
		return "sign/convertToLocal";
	}
	
}
