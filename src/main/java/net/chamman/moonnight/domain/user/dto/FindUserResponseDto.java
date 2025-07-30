package net.chamman.moonnight.domain.user.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import net.chamman.moonnight.auth.oauth.OAuthResponseDto;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.domain.user.User.UserProvider;
import net.chamman.moonnight.domain.user.User.UserStatus;

@Builder
public record FindUserResponseDto(
		UserProvider userProvider, 
		List<OAuthResponseDto> oauthResponseDtoList,
		String email, 
		UserStatus userStatus,
		LocalDateTime createdAt
		) {

	public static FindUserResponseDto fromEntity(UserResponseDto userResponseDto) {
		return FindUserResponseDto.builder()
				.userProvider(userResponseDto.userProvider())
				.oauthResponseDtoList(userResponseDto.oauthResponseDtoList())
				.email(userResponseDto.email())
				.userStatus(userResponseDto.userStatus())
				.createdAt(userResponseDto.createdAt())
				.build();
	}
}
