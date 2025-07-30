package net.chamman.moonnight.infra.email.impl;

import static net.chamman.moonnight.global.exception.HttpStatusCode.EMAIL_SEND_FAIL;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.exception.infra.EmailSendException;
import net.chamman.moonnight.global.util.LogMaskingUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil.MaskLevel;
import net.chamman.moonnight.infra.email.EmailSender;
import net.chamman.moonnight.infra.email.naver.NaverMailRequestDto;
import net.chamman.moonnight.infra.email.naver.NaverMailRecipientRequestDto;
import net.chamman.moonnight.infra.naver.NaverSignatureGenerator;

@Component
@RequiredArgsConstructor
@Slf4j
@PropertySource("classpath:application.properties")
public class NaverMailClient implements EmailSender{
	
	private final NaverSignatureGenerator naverSignatureGenerator;
	
	@Value("${naver-api.accessKey}")
	private String accessKey;
	@Value("${naver-email.senderEmail}")
	private String senderEmail;
	
	private final String emailApiUrl = "https://mail.apigw.ntruss.com";
	private final String emailSendPath = "/api/v1/mails";
	
	@Override
	public int sendEmail(String recipientEmail, String title, String message) {
		
		log.debug("* Naver Mail 발송 요청. Recipient: [{}], Title: [{}]",
				LogMaskingUtil.maskEmail(recipientEmail, MaskLevel.MEDIUM),
				title);
		
		// 수신자 설정
		NaverMailRecipientRequestDto naverMailRecipientRequestDto = new NaverMailRecipientRequestDto(recipientEmail,recipientEmail,"R");
		List<NaverMailRecipientRequestDto> mails = List.of(naverMailRecipientRequestDto);
		
		// 요청 데이터 생성
		NaverMailRequestDto naverMailRequestDto = NaverMailRequestDto.builder()
				.senderAddress(senderEmail)
				.senderName("달밤 청소")
				.title(title)
				.body(message)
				.advertising(false)
				.recipients(mails)
				.build();
		
		try {
			
			String time = Long.toString(System.currentTimeMillis());
			
			// 헤더 설정
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("x-ncp-apigw-timestamp", time);
			headers.set("x-ncp-iam-access-key", accessKey);
			headers.set("x-ncp-apigw-signature-v2", naverSignatureGenerator.getNaverSignature("POST", emailSendPath, time));
			
			// HTTP 요청
			RestTemplate restTemplate = new RestTemplate();
			HttpEntity<NaverMailRequestDto> entity = new HttpEntity<>(naverMailRequestDto, headers);
			ResponseEntity<String> response = restTemplate.exchange(new URI(emailApiUrl + emailSendPath),
					HttpMethod.POST, entity, String.class);
			
			
			return response.getStatusCode().value();
		} catch (Exception e) {
			throw new EmailSendException(EMAIL_SEND_FAIL,e);
		}
	}
	
}
