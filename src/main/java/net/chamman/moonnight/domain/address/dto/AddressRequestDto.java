package net.chamman.moonnight.domain.address.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import net.chamman.moonnight.domain.address.Address;
import net.chamman.moonnight.domain.user.User;

public record AddressRequestDto(
    @NotBlank(message = "validation.address.name.required")
    @Size(max = 20, message = "validation.address.name.length")
    String name,
    
    @NotBlank(message = "validation.address.postcode.required")
    @Size(max = 10, message = "validation.address.invalid")
    String postcode,
    
    @NotBlank(message = "validation.address.main_address.required")
    @Size(max = 255, message = "validation.address.invalid")
    String mainAddress,
    
    @NotBlank(message = "validation.address.detail_address.required")
    @Size(max = 255, message = "validation.address.invalid")
    String detailAddress
    ) {

  public Address toEntity(User user) {
    return Address.builder()
        .user(user)
        .name(name)
        .postcode(postcode)
        .mainAddress(mainAddress)
        .detailAddress(detailAddress)
        .build();
}
}
