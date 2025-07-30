package net.chamman.moonnight.auth.verification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerificationEmailRequestDto(
    @NotBlank(message = "validation.user.email.required")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$", message = "validation.user.email.invalid")
    @Size(min = 5, max = 50, message = "{validation.user.email.length}")
    String email,
    
    @NotBlank(message = "validation.verification.code.required")
    @Pattern(regexp = "^\\d{6}$", message = "validation.verification.code.invalid")
    @Size(max = 6, message = "validation.verification.code.length")
    String verificationCode
    ) {

}
