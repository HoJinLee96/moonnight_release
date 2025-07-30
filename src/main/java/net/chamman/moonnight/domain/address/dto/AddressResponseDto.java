package net.chamman.moonnight.domain.address.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.domain.address.Address;

@Builder
public record AddressResponseDto(
    
    int addressId,
    String name,
    String postcode,
    String mainAddress,
    String detailAddress,
    boolean isPrimary,
    LocalDateTime createdAt, 
    LocalDateTime updatedAt
    
    ) {

  public static AddressResponseDto fromEntity(Address address, Obfuscator obfuscator) {
    return AddressResponseDto.builder()
    .addressId(obfuscator.encode(address.getAddressId()))
    .name(address.getName())
    .postcode(address.getPostcode())
    .mainAddress(address.getMainAddress())
    .detailAddress(address.getDetailAddress())
    .isPrimary(address.isPrimary())
    .createdAt(address.getCreatedAt())
    .updatedAt(address.getUpdatedAt())
    .build();
  }
}
