package net.chamman.moonnight.infra.sms;

public interface SmsSender {
	int sendSms(String recipientPhone, String message);
}
