package net.chamman.moonnight.domain.user;

import java.time.LocalDateTime;
import org.hibernate.annotations.Generated; 
import org.hibernate.generator.EventType;
import org.hibernate.validator.constraints.UniqueElements;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user", uniqueConstraints = {@UniqueConstraint(name = "UK_user_email_user_provider", columnNames = {"email", "user_provider"}) })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private int userId;
	
	@Column(name = "user_provider", nullable=false)
	@Enumerated(EnumType.STRING)
	@Basic(fetch = FetchType.EAGER)
	private UserProvider userProvider;
	
	@Column(name = "email", length=50, nullable=false)
	private String email;
	
	@Column(name = "password", length=60)
	private String password;
	
	@Column(name = "name", length=20)
	private String name;
	
	@Column(name = "birth", length=10)
	private String birth;
	
	@Column(name = "phone", length=15)
	private String phone;
	
	@Column(name = "user_status", nullable=false)
	@Enumerated(EnumType.STRING)
	@Basic(fetch = FetchType.EAGER)
	private UserStatus userStatus;
	
	@Column(name = "marketing_received_status")
	private Boolean marketingReceivedStatus;
	
	@Generated(event = EventType.INSERT)
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
	
	@Generated(event = EventType.UPDATE)
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
	
	
	public static enum UserProvider {
		LOCAL, OAUTH
	}
	
	public static enum UserStatus {
		ACTIVE, STAY, STOP, DELETE;
	}
}
