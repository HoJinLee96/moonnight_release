package net.chamman.moonnight.infra.sms.impl;

import static net.chamman.moonnight.global.exception.HttpStatusCode.SMS_SEND_FAIL;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.exception.infra.SmsSendException;
import net.chamman.moonnight.global.util.LogMaskingUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil.MaskLevel;
import net.chamman.moonnight.infra.naver.NaverSignatureGenerator;
import net.chamman.moonnight.infra.sms.SmsSender;
import net.chamman.moonnight.infra.sms.naver.NaverSmsRequestDto;
import net.chamman.moonnight.infra.sms.naver.NaverSmsRecipientRequestDto;

@Component
@RequiredArgsConstructor
@Slf4j
@PropertySource("classpath:application.properties")
public class NaverSmsClient implements SmsSender{
	
	private final NaverSignatureGenerator naverSignatureGenerator;
	
	@Value("${naver-api.accessKey}")
	private String accessKey;
	
	@Value("${naver-sms.serviceId}")
	private String serviceId;
	
	@Value("${naver-sms.senderPhone}")
	private String senderPhone;
	
	private final String smsApiUrl = "https://sens.apigw.ntruss.com";
	private String smsSendPath;
	
	@PostConstruct
	private void initSmsPath() {
		this.smsSendPath = "/sms/v2/services/" + serviceId + "/messages";
	}
	
	/** 네이버 API 문자 발송 요청
	 * @param naverSmsPayload
	 * @throws SmsSendException {@link #sendVerificationCode}
	 * @return 문자 발송 요청 Response Status Code
	 */
	@Override
	public int sendSms(String recipientPhone, String message) {
		
		log.debug("* Naver SMS 발송 요청. Recipient: [{}], message: [{}]",
				LogMaskingUtil.maskPhone(recipientPhone, MaskLevel.MEDIUM),
				LogMaskingUtil.maskText(message, MaskLevel.MEDIUM)
				);

		NaverSmsRecipientRequestDto naverSmsRecipientRequestDto = new NaverSmsRecipientRequestDto(recipientPhone.replaceAll("[^0-9]", ""),message);
		List<NaverSmsRecipientRequestDto> messages = List.of(naverSmsRecipientRequestDto);
		
		NaverSmsRequestDto naverSmsRequestDto = NaverSmsRequestDto.builder()
				.type("SMS")
				.contentType("COMM")
				.countryCode("82")
				.from(senderPhone)
				.content(message)
				.messages(messages)
				.build();
		
		try {
			String time = Long.toString(System.currentTimeMillis());
			
			// 헤더 설정
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("x-ncp-apigw-timestamp", time);
			headers.set("x-ncp-iam-access-key", accessKey);
			headers.set("x-ncp-apigw-signature-v2", naverSignatureGenerator.getNaverSignature("POST", smsSendPath, time));
			
			// HTTP 요청
			RestTemplate restTemplate = new RestTemplate();
			HttpEntity<NaverSmsRequestDto> entity = new HttpEntity<>(naverSmsRequestDto, headers);
			ResponseEntity<String> response = restTemplate.postForEntity(new URI(smsApiUrl + smsSendPath),
					entity, String.class);
			
			return response.getStatusCode().value();
		} catch (Exception e) {
			throw new SmsSendException(SMS_SEND_FAIL, e);
		}
	}
	
}
