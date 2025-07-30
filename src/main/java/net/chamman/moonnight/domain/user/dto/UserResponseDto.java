package net.chamman.moonnight.domain.user.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import net.chamman.moonnight.auth.oauth.OAuthResponseDto;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.domain.user.User.UserProvider;
import net.chamman.moonnight.domain.user.User.UserStatus;

@Builder
public record UserResponseDto(
		UserProvider userProvider,
		List<OAuthResponseDto> oauthResponseDtoList,
		String email,
		String name,
		String birth,
		String phone,
		UserStatus userStatus,
		boolean marketingReceivedStatus,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
		
		) 
{
	
	public static UserResponseDto fromEntity(User user, List<OAuthResponseDto> oauthResponseDtoList) {
		return UserResponseDto.builder()
				.userProvider(user.getUserProvider())
				.oauthResponseDtoList(oauthResponseDtoList)
				.email(user.getEmail())
				.name(user.getName())
				.birth(user.getBirth())
				.phone(user.getPhone())
				.userStatus(user.getUserStatus())
				.marketingReceivedStatus(user.getMarketingReceivedStatus())
				.createdAt(user.getCreatedAt())
				.updatedAt(user.getUpdatedAt())
				.build();
	}
}
