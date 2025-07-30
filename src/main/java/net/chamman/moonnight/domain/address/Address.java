package net.chamman.moonnight.domain.address;

import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import net.chamman.moonnight.domain.address.dto.AddressRequestDto;
import net.chamman.moonnight.domain.user.User;

@Entity
@Table(name = "address")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "address_id")
	private int addressId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "FK_user_TO_address_1"))
	private User user;
	
	@Column(name = "name", length = 20)
	private String name;
	
	@Column(name = "postcode", length = 10, nullable = false)
	private String postcode;
	
	@Column(name = "main_address", length = 250, nullable = false)
	private String mainAddress;
	
	@Column(name = "detail_address", length = 250, nullable = false)
	private String detailAddress;
	
	@Column(name = "is_primary")
	@ColumnDefault("false") 
	private boolean isPrimary;
	
	@Generated(event = EventType.INSERT)
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
	
	@Generated(event = EventType.UPDATE)
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
	
	public void update(AddressRequestDto addressRequestDto) {
		this.name=addressRequestDto.name();
		this.postcode=addressRequestDto.postcode();
		this.mainAddress=addressRequestDto.mainAddress();
		this.detailAddress=addressRequestDto.detailAddress();
	}
}
