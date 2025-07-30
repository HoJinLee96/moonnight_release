package net.chamman.moonnight.auth.oauth;

import java.time.LocalDateTime;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.chamman.moonnight.domain.user.User;


//CREATE TABLE `oauth` (
//    `oauth_id` INT AUTO_INCREMENT PRIMARY KEY,
//    `user_id`  INT NOT NULL,
//    `oauth_provider`  ENUM ("NAVER", "KAKAO") NOT NULL,
//    `oauth_provider_id`    VARCHAR(255) UNIQUE NOT NULL,
//    `oauth_status`    ENUM("ACTIVE","STAY","STOP","DELETE") DEFAULT "ACTIVE",
//    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
//    `updated_at` DATETIME NULL ON UPDATE CURRENT_TIMESTAMP
//  );
//  ALTER TABLE `oauth` ADD CONSTRAINT `FK_user_TO_oauth_1`
//  FOREIGN KEY ( `user_id` )
//  REFERENCES `user` ( `user_id` );
  
  
@Entity
@Table(name = "oauth")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OAuth {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "oauth_id") 
	private int oauthId;
	
	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "FK_user_TO_address_1"))
	private User user;
	
	@Column(name = "oauth_provider", nullable=false)
	@Enumerated(EnumType.STRING)
	@Basic(fetch = FetchType.EAGER)
	private OAuthProvider oauthProvider;
	
	@Column(name = "oauth_provider_id", length=255, nullable=false)
	private String oauthProviderId;
	
	@Column(name = "oauth_status", nullable=false)
	@Enumerated(EnumType.STRING)
	@Basic(fetch = FetchType.EAGER)
	private OAuthStatus oauthStatus;
	
	@Generated(event = EventType.INSERT)
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
	
	@Generated(event = EventType.UPDATE)
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
	
	public enum OAuthProvider {
		NAVER, KAKAO
	}
	
	public enum OAuthStatus {
		ACTIVE, STAY, STOP
	}
	
}
