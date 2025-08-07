package net.chamman.moonnight.domain.admin;

import java.time.LocalDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import org.springframework.data.annotation.Version;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "admin")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Admin {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "admin_id")
	private int adminId;
	
	@Column(name = "email", length=50, nullable=false)
	private String email;
	
	@Column(name = "password", length=60)
	private String password;
	
	@Column(name = "name", length=20)
	private String name;
	
	@Column(name = "phone", length=15)
	private String phone;
	
	@Column(name = "admin_status", nullable=false)
	@Enumerated(EnumType.STRING)
	@Basic(fetch = FetchType.EAGER)
	private AdminStatus adminStatus;
	
	@Generated(event = EventType.INSERT)
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
	
	@Generated(event = EventType.UPDATE)
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
	
	@Version
	Long version;
	
	public static enum AdminStatus {
		ACTIVE, STAY, STOP, DELETE;
	}
}
