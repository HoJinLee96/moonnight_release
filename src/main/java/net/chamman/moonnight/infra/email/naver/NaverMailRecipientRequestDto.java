package net.chamman.moonnight.infra.email.naver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NaverMailRecipientRequestDto {
	
	String address;
	String name;
	String type;
	
}
