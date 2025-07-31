package net.chamman.moonnight.domain.admin.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import net.chamman.moonnight.domain.admin.Admin.AdminStatus;

@Builder
public record FindAdminResponseDto(
		String email, 
		AdminStatus adminStatus,
		LocalDateTime createdAt
		) {

	public static FindAdminResponseDto fromEntity(AdminResponseDto adminResponseDto) {
		return FindAdminResponseDto.builder()
				.email(adminResponseDto.email())
				.adminStatus(adminResponseDto.adminStatus())
				.createdAt(adminResponseDto.createdAt())
				.build();
	}
}
