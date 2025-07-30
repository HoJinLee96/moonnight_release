package net.chamman.moonnight.domain.address;

import static net.chamman.moonnight.global.exception.HttpStatusCode.CREATE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.DELETE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.READ_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.READ_SUCCESS_NO_DATA;
import static net.chamman.moonnight.global.exception.HttpStatusCode.UPDATE_SUCCESS;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.domain.address.dto.AddressRequestDto;
import net.chamman.moonnight.domain.address.dto.AddressResponseDto;
import net.chamman.moonnight.global.security.principal.CustomUserDetails;
import net.chamman.moonnight.global.util.ApiResponseDto;
import net.chamman.moonnight.global.util.ApiResponseFactory;

@Tag(name = "AddressController", description = "주소 정보 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/address")
public class AddressController {
	
	private final AddressService addressService;
	private final ApiResponseFactory apiResponseFactory;
	
	@Operation(summary = "주소 등록", description = "AddressRequestDto 통해 주소 등록.")
	@PreAuthorize("hasRole('OAUTH') or hasRole('LOCAL')")
	@PostMapping("/private/register")
	public ResponseEntity<ApiResponseDto<AddressResponseDto>> registerAddress(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@Valid @RequestBody AddressRequestDto addressDto){
		
		AddressResponseDto addressResponseDto = addressService.registerAddress(userDetails.getUserId(), addressDto);
		
		return ResponseEntity.ok(apiResponseFactory.success(CREATE_SUCCESS, addressResponseDto));
	}
	
	@Operation(summary = "주소 조회", description = "유저 단일 주소 조회")
	@PreAuthorize("hasRole('OAUTH') or hasRole('LOCAL')")
	@GetMapping("/private/{addressId}")
	public ResponseEntity<ApiResponseDto<AddressResponseDto>> getAddress(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable("addressId") int encodedAddressId) {
		
		AddressResponseDto addressResponseDto = addressService.getAddress(userDetails.getUserId(), encodedAddressId);
		
		return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS, addressResponseDto));
	}
	
	@Operation(summary = "주소 리스트 조회", description = "유저 주소 리스트 조회")
	@PreAuthorize("hasRole('OAUTH') or hasRole('LOCAL')")
	@GetMapping("/private/getList")
	public ResponseEntity<ApiResponseDto<List<AddressResponseDto>>> getAddressList(
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		
		List<AddressResponseDto> list = addressService.getAddressList(userDetails.getUserId());
		
		if(list==null || list.isEmpty() || list.size()==0) {
			return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS_NO_DATA,null));
		}
		
		return ResponseEntity.ok(apiResponseFactory.success(READ_SUCCESS,list));
	}
	
	@Operation(summary = "주소 수정", description = "주소 수정")
	@PreAuthorize("hasRole('OAUTH') or hasRole('LOCAL')")
	@PatchMapping("/private/{addressId}")
	public ResponseEntity<ApiResponseDto<Void>> updateAddress(
			@AuthenticationPrincipal CustomUserDetails userDetail,
			@PathVariable("addressId") int encodedAddressId,
			@Valid @RequestBody AddressRequestDto addressRequestDto) {
		
		addressService.updateAddress(userDetail.getUserId(), encodedAddressId, addressRequestDto);
		
		return ResponseEntity.ok(apiResponseFactory.success(UPDATE_SUCCESS));  
	}
	
	@Operation(summary = "대표 주소 설정", description = "유저 본인 주소 대표 주소 설정")
	@PreAuthorize("hasRole('OAUTH') or hasRole('LOCAL')")
	@PatchMapping("/private/primary/{addressId}")
	public ResponseEntity<ApiResponseDto<Void>> updatePrimary(
			@AuthenticationPrincipal CustomUserDetails userDetail,
			@PathVariable("addressId") int encodedAddressId) {
		
		addressService.updatePrimary(userDetail.getUserId(), encodedAddressId);
		
		return ResponseEntity.ok(apiResponseFactory.success(UPDATE_SUCCESS));
	}
	
	@Operation(summary = "주소 삭제", description = "유저 본인 주소 삭제")
	@PreAuthorize("hasRole('OAUTH') or hasRole('LOCAL')")
	@DeleteMapping("/private/{addressId}")
	public ResponseEntity<ApiResponseDto<Void>> deleteAddress(
			@AuthenticationPrincipal CustomUserDetails userDetail,
			@PathVariable("addressId") int encodedAddressId) {
		
		addressService.deleteAddress(userDetail.getUserId(), encodedAddressId);
		
		return ResponseEntity.ok(apiResponseFactory.success(DELETE_SUCCESS));  
	}
	
}
