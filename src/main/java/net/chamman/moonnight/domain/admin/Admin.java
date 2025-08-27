package net.chamman.moonnight.domain.admin;

import static net.chamman.moonnight.global.exception.HttpStatusCode.USER_STATUS_DELETE;
import static net.chamman.moonnight.global.exception.HttpStatusCode.USER_STATUS_STAY;
import static net.chamman.moonnight.global.exception.HttpStatusCode.USER_STATUS_STOP;
import static net.chamman.moonnight.global.exception.HttpStatusCode.VERSION_MISMATCH;

import java.time.LocalDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

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
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.chamman.moonnight.global.exception.VersionMismatchException;
import net.chamman.moonnight.global.exception.status.StatusDeleteException;
import net.chamman.moonnight.global.exception.status.StatusStayException;
import net.chamman.moonnight.global.exception.status.StatusStopException;

@Entity
@Table(name = "admin")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Admin {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "admin_id")
	private int adminId;

	@Column(name = "email", length = 50, nullable = false)
	private String email;

	@Setter
	@Column(name = "password", length = 60)
	private String password;

	@Column(name = "name", length = 20)
	private String name;

	@Column(name = "phone", length = 15)
	private String phone;

	@Setter
	@Column(name = "admin_status", nullable = false)
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
	int version;

	public static enum AdminStatus {
		ACTIVE, STAY, STOP, DELETE;
	}

	@SuppressWarnings("incomplete-switch")
	public void validateStatus() {
		switch (this.adminStatus) {
		case STAY -> throw new StatusStayException(USER_STATUS_STAY, "일시 정지된 계정. admin.id: " + this.adminId);
		case STOP -> throw new StatusStopException(USER_STATUS_STOP, "정지된 계정. admin.id: " + this.adminId);
		case DELETE -> throw new StatusDeleteException(USER_STATUS_DELETE, "탈퇴한 계정. admin.id: " + this.adminId);
		}
	}

	public void isStay() {
		if (this.adminStatus.equals(AdminStatus.STOP)) {
			throw new StatusStayException(USER_STATUS_STAY, "일시 정지된 계정. admin.id: " + this.adminId);
		}
	}

	public void isStop() {
		if (this.adminStatus.equals(AdminStatus.STOP)) {
			throw new StatusStopException(USER_STATUS_STOP, "정지된 계정. admin.id: " + this.adminId);
		}
	}

	public void isDelete() {
		if (this.adminStatus.equals(AdminStatus.DELETE)) {
			throw new StatusDeleteException(USER_STATUS_DELETE, "탈퇴한 계정. admin.id: " + this.adminId);
		}
	}

	public void verifyVersion(int version) {
		if (this.version != version) {
			throw new VersionMismatchException(VERSION_MISMATCH);
		}
	}

	public void modify(String name, String phone) {
		if (name != null && !name.isBlank()) {
			this.name = name;
		}
		if (phone != null && !phone.isBlank()) {
			this.phone = phone;
		}
	}

	public void softDelete() {
		this.email = this.email + "_" + adminId + "_deleted";
		this.adminStatus = AdminStatus.DELETE;
		this.name = "탈퇴한관리자";
	}
}
