package net.chamman.moonnight.auth.verification;

import java.time.LocalDateTime;
import java.util.Arrays;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "verification")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Verification {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "verification_id")
	private int verificationId;
	
	@Column(name = "client_ip", length = 50, nullable = false)
	private String clientIp;
	
	@Column(name = "recipient", length = 50, nullable = false)
	private String recipient; 
	
	@Column(name = "verification_code", length = 6, nullable = false)
	private String verificationCode;
	
	@Column(name = "send_status", nullable = false)
	private int sendStatus;
	
	@Generated(event = EventType.INSERT)
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
	
	@Column(name = "verify")
	private boolean verify;
	
	@Column(name = "verify_at")
	private LocalDateTime verifyAt; 
	
	public SendStatus getSendStatusEnum() {
		return SendStatus.fromCode(this.sendStatus);
	}
	
	public enum SendStatus {
		Informational(1),
		SUCCESS(2),
		Redirection(3),
		FAILURE(4), 
		SERVER_ERROR(5),
		UNKNOWN(-1);  
		
		private final int category;
		
		SendStatus(int category) {
			this.category = category;
		}
		
		public static SendStatus fromCode(int code) {
			int firstDigit = code / 100; 
			
			return Arrays.stream(values())
					.filter(status -> status.category == firstDigit)
					.findFirst()
					.orElse(UNKNOWN); 
		}
		
		public int getCategory() {
			return category;
		}
	}

}