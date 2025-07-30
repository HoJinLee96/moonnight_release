package net.chamman.moonnight.auth.sign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import net.chamman.moonnight.domain.address.Address;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.domain.user.User.UserStatus;

public record SignUpRequestDto(
    
    @NotBlank(message = "{validation.user.name.required}")
    @Pattern(regexp = "^[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9 ]+$", message = "{validation.user.name.invalid}")
    @Size(min = 2, max = 20, message = "{validation.user.name.length}")
    String name,
    
    @NotBlank(message = "{validation.user.birth.required}")
    @Pattern(regexp = "^\\d{8}$", message = "{validation.user.birth.invalid}")
    String birth,
    
    @NotBlank(message = "{validation.user.phone.required}")
    @Pattern(regexp = "^\\d{3,4}-\\d{3,4}-\\d{4}$", message = "{validation.user.phone.invalid}")
    String phone,
    
    @NotBlank(message = "{validation.address.postcode.required}")
    @Size(max = 10, message = "{validation.address.invalid}")
    String postcode,
    
    @NotBlank(message = "validation.address.main_address.required")
    @Size(max = 255, message = "{validation.address.invalid}")
    String mainAddress,
    
    @NotBlank(message = "validation.address.detail_address.required")
    @Size(max = 255, message = "{validation.address.invalid}")
    String detailAddress,
    
    boolean marketingReceivedStatus
) {

  public User toEntity() {
      return User.builder()
          .name(name)
          .birth(birth)
          .phone(phone)
          .userStatus(UserStatus.ACTIVE)
          .marketingReceivedStatus(marketingReceivedStatus)
          .build();
  }
  
  public Address toAddressEntity(User user) {
    return Address.builder()
    		.user(user)
	        .name(name)
	        .postcode(postcode)
	        .mainAddress(mainAddress)
	        .detailAddress(detailAddress)
	        .isPrimary(true)
	        .build();
  }
}