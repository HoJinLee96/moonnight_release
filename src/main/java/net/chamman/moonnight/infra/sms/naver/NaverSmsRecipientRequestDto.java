package net.chamman.moonnight.infra.sms.naver;

public class NaverSmsRecipientRequestDto {
	
	private String to;
	private String content;
	
	public NaverSmsRecipientRequestDto() {
	}
	
	public NaverSmsRecipientRequestDto(String to) {
		this.to = to;
	}
	
	public NaverSmsRecipientRequestDto(String to, String content) {
		this.to = to;
		this.content = content;
	}
	
	public String getTo() {
		return to;
	}
	
	public String getContent() {
		return content;
	}
	
}
