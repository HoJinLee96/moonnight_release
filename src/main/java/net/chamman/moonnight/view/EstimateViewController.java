package net.chamman.moonnight.view;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.token.JwtProvider;
import net.chamman.moonnight.auth.token.TokenProvider;
import net.chamman.moonnight.domain.address.AddressService;
import net.chamman.moonnight.domain.address.dto.AddressResponseDto;
import net.chamman.moonnight.domain.estimate.EstimateService;
import net.chamman.moonnight.domain.estimate.dto.EstimateResponseDto;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.domain.user.UserService;
import net.chamman.moonnight.domain.user.dto.UserResponseDto;
import net.chamman.moonnight.global.security.principal.CustomUserDetails;
import net.chamman.moonnight.global.util.CookieUtil;

@Controller
@Slf4j
@RequiredArgsConstructor
public class EstimateViewController {

	private final UserService userService;
	private final AddressService addressService;
	private final EstimateService estimateService;
	private final TokenProvider tokenProvider;
	private final JwtProvider jwtProvider;

	@GetMapping("/estimate/register")
	public String showEstimate(@AuthenticationPrincipal CustomUserDetails customUserDetails, Model model) {

		if (customUserDetails != null) {
			User user = userService.getActiveUserByUserId(customUserDetails.getUserId());
			List<AddressResponseDto> addressList = addressService.getAddressList(customUserDetails.getUserId());
			model.addAttribute("user", UserResponseDto.fromEntity(user,null));
			model.addAttribute("addressList", addressList);
			log.debug("addressList: {}", addressList.toArray().toString());
			if (addressList != null && !addressList.isEmpty()) {
				AddressResponseDto primaryAddress = addressList.stream().filter(AddressResponseDto::isPrimary)
						.findFirst().orElse(addressList.get(0));
				model.addAttribute("primaryAddress", primaryAddress);
				log.debug("primaryAddress: {}", primaryAddress);
			}
		}

		return "estimate/estimateRegister";
	}

	@GetMapping("/estimate/search")
	public String showEstimateSearch(HttpServletRequest req, HttpServletResponse res, Model model) {
		String authToken = null ;
		Cookie cookie = WebUtils.getCookie(req, "X-Auth-Token");
		if (cookie != null) {
			authToken = cookie.getValue();
		}
		log.debug("* JwtAuthFilter 토큰 널 체크.");
		if (authToken == null || authToken.isBlank()) {
			CookieUtil.addCookie(res, "X-Auth-Token", "", Duration.ZERO);
			return "estimate/estimateSearch";
		}

		log.debug("* JwtAuthFilter 토큰 블랙리스트 체크.");
		String value = tokenProvider.getBlackListValue(authToken);
		if (value != null) {
			CookieUtil.addCookie(res, "X-Auth-Token", "", Duration.ZERO);
			return "estimate/estimateSearch";
		}
		Map<String, Object> claims = jwtProvider.validateAuthToken(authToken);
		String recipient = (String) claims.get("recipient");
		List<EstimateResponseDto> list = estimateService.getAllEstimateByAuth(recipient);
		model.addAttribute("estimateList", list);
		return "estimate/estimateSearch";
	}

}
