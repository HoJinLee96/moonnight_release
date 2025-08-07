package net.chamman.moonnight.domain.estimate;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import lombok.ToString;
import net.chamman.moonnight.domain.estimate.dto.EstimateRequestDto;
import net.chamman.moonnight.global.util.StringListConverter;

@Entity
@Table(name = "estimate")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Estimate {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "estimate_id")
	private int estimateId;

	@Column(name = "name", length = 20, nullable = false)
	private String name;

	@Column(name = "phone", length = 20)
	private String phone;

	@Column(name = "email", length = 50)
	private String email;

	@Column(name = "email_agree")
	private boolean emailAgree;

	@Column(name = "phone_agree")
	private boolean phoneAgree;

	@Column(name = "postcode", length = 10)
	private String postcode;

	@Column(name = "main_address", length = 255, nullable = false)
	private String mainAddress;

	@Column(name = "detail_address", length = 255)
	private String detailAddress;

	@Column(name = "cleaning_service", length = 30, nullable = false)
	private String cleaningService;

	@Column(name = "content", length = 5000)
	private String content;

	@Column(name = "images_path", length = 5000)
	@Convert(converter = StringListConverter.class) 
	private List<String> imagesPath;

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

	public enum CleaningService {
		// 주거 및 단건 청소
		NEW_BUILDING("신축 청소"), 
		MOVING("이사 청소"), 
		RESIDENTIAL("거주 청소"), 
		REMODELING("리모델링 청소"),
		COMPLETION("준공 청소"),

		// 상업/시설 단건 청소
		SHOP("상가 청소"), 
		OFFICE("사무실 청소"), 
		BUILDING("건물 청소"), 
		STAIR("계단 청소"), 
		TOILET("화장실 청소"),
		ACADEMY("학원 청소"),
		LARGE("대형 청소"),

		// 정기 청소
		STAIR_REGULAR("계단 정기청소"), 
		SHOP_REGULAR("상가 정기청소"), 
		OFFICE_REGULAR("사무실 정기청소"),
		HOSPITAL_REGULAR("병원 정기청소"), 
		ACADEMY_REGULAR("학원 정기청소"), 
		TOILET_REGULAR("화장실 정기청소");

		private final String label;

		CleaningService(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}
	}

	public enum EstimateStatus {
	    ALL("전체"),
	    RECEIVE("접수"),
	    IN_PROGRESS("처리중"),
	    COMPLETE("완료"),
	    DELETE("삭제");

	    private final String label;

	    EstimateStatus(String label) {
	        this.label = label;
	    }

	    public String getLabel() {
	        return label;
	    }
	}

	public void updateEstimate(EstimateRequestDto estimateRequestDto, List<String> imagesPath) {
		this.name=estimateRequestDto.name();
		this.email=estimateRequestDto.email();
		this.emailAgree=estimateRequestDto.emailAgree();
		this.phoneAgree=estimateRequestDto.phoneAgree();
		this.postcode=estimateRequestDto.postcode();
		this.mainAddress=estimateRequestDto.mainAddress();
		this.detailAddress=estimateRequestDto.detailAddress();
		this.content=estimateRequestDto.content();
		this.cleaningService=estimateRequestDto.cleaningService().name();
		this.imagesPath=imagesPath;
	}
}
