package net.chamman.moonnight.global.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.infra.email.EmailSender;
import net.chamman.moonnight.infra.sms.SmsSender;

@Service
@RequiredArgsConstructor
@PropertySource("classpath:application.properties")
@Slf4j
public class NotificationService {
	
	private final SmsSender smsSender;
	private final EmailSender emailSender;
	
	@Value("${admin.phone}")
	private String adminPhone;
	@Value("${admin.email}")
	private String adminEmail;
	
	
	/** 서버 문제 발생 관리자 문자 및 이메일 알람
	 * @param message
	 */
	public void sendAdminAlert(String message) {
		
		try {
			int smsStatusCode = smsSender.sendSms(adminPhone, message);
			if((smsStatusCode/100)!=2) {
				log.error("달밤 청소 서버 문제 발생. 관리자 문자 알람 발송 실패.");
			}
			int emailStatusCode = emailSender.sendEmail(adminEmail,"달밤 청소 서버 문제 발생",message);
			if((emailStatusCode/100)!=2) {
				log.error("달밤 청소 서버 문제 발생. 관리자 이메일 알람 발송 실패.");
			}
		} catch (Exception e) {
			log.error("서버 문제 발생. 관리자 문자 알람 발송 실패. e: {}", e);
		}
		
	}
}
