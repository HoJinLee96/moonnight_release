package net.chamman.moonnight.auth.adminSign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import net.chamman.moonnight.domain.admin.Admin;
import net.chamman.moonnight.domain.admin.Admin.AdminStatus;

public record AdminSignUpRequestDto(
    
    @NotBlank(message = "{validation.user.name.required}")
    @Pattern(regexp = "^[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9 ]+$", message = "{validation.user.name.invalid}")
    @Size(min = 2, max = 20, message = "{validation.user.name.length}")
    String name,
    
    @NotBlank(message = "{validation.user.phone.required}")
    @Pattern(regexp = "^\\d{3,4}-\\d{3,4}-\\d{4}$", message = "{validation.user.phone.invalid}")
    String phone
) {

  public Admin toEntity() {
      return Admin.builder()
          .name(name)
          .phone(phone)
          .adminStatus(AdminStatus.ACTIVE)
          .build();
  }
}