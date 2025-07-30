package net.chamman.moonnight.domain.estimate.simple;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.chamman.moonnight.domain.estimate.Estimate.CleaningService;
import net.chamman.moonnight.domain.estimate.Estimate.EstimateStatus;
import net.chamman.moonnight.domain.user.User;

@Entity
@Table(name = "simple_estimate")
@Builder
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class SimpleEstimate {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "simple_estimate_id")
	private int simpleEstimateId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "FK_user_TO_simple_estimate_1"))
	private User user;
	
	@Column(name = "phone", length = 20, nullable = false)
	private String phone;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "cleaning_service", nullable = false)
	private CleaningService cleaningService;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "region", nullable = false)
	private Region region;
	
	@Enumerated(EnumType.STRING)
	@Basic(fetch = FetchType.EAGER)
	@Column(name = "estimate_status", nullable = false)
	private EstimateStatus estimateStatus;
	
	@Column(name = "client_ip", length = 50, nullable = false)
	private String clientIp;
	
	@Generated(event = EventType.INSERT)
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
	
	@Generated(event = EventType.UPDATE)
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
	
	public enum Region{
		서울, 부산, 대구, 인천, 광주, 대전, 울산, 세종, 경기, 강원, 충북, 충남, 전북, 전남, 경북, 경남, 제주
	}
}
