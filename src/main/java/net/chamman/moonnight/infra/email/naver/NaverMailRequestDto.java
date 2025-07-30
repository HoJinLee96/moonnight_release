package net.chamman.moonnight.infra.email.naver;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NaverMailRequestDto {
	String senderAddress;
	String senderName;
	String title;
	String body;
	boolean advertising;
	List<NaverMailRecipientRequestDto> recipients;
}
