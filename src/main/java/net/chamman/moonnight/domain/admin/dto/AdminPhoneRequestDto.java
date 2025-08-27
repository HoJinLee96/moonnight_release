package net.chamman.moonnight.domain.admin.dto;

import net.chamman.moonnight.global.annotation.ValidPhone;

public record AdminPhoneRequestDto(
		@ValidPhone
		String phone,
		int version
		) {

}
