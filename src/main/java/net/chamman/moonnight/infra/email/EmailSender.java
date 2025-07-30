package net.chamman.moonnight.infra.email;

public interface EmailSender {
	int sendEmail(String recipientEmail, String title, String message);
}
