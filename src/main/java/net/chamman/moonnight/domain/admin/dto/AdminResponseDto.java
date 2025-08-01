package net.chamman.moonnight.domain.admin.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import net.chamman.moonnight.domain.admin.Admin;
import net.chamman.moonnight.domain.admin.Admin.AdminStatus;

@Builder
public record AdminResponseDto(
		
		String email,
		String name,
		String phone,
		AdminStatus adminStatus,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
		
		) 
{
	
	public static AdminResponseDto fromEntity(Admin admin) {
		return AdminResponseDto.builder()
				.email(admin.getEmail())
				.name(admin.getName())
				.phone(admin.getPhone())
				.adminStatus(admin.getAdminStatus())
				.createdAt(admin.getCreatedAt())
				.updatedAt(admin.getUpdatedAt())
				.build();
	}
}
