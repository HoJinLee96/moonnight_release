package net.chamman.moonnight.domain.comment;

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
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.chamman.moonnight.domain.admin.Admin;
import net.chamman.moonnight.domain.estimate.Estimate;
import net.chamman.moonnight.global.exception.VersionMismatchException;

@Entity
@Table(name = "comment")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "comment_id")
	private int commentId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "admin_id", nullable = false, foreignKey = @ForeignKey(name = "FK_admin_TO_comment_1"))
	private Admin admin;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "estimate_id", nullable = false, foreignKey = @ForeignKey(name = "FK_estimate_TO_comment_1"))
	private Estimate estimate;

	@Column(name = "comment_text", length = 250, nullable = false)
	private String commentText;

	@Enumerated(EnumType.STRING)
	@Basic(fetch = FetchType.EAGER)
	@Column(name = "comment_status", nullable = false)
	private CommentStatus commentStatus;

	@Generated(event = EventType.INSERT)
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@Generated(event = EventType.UPDATE)
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Version
	int version;

	public enum CommentStatus {
		ACTIVE, DELETE
	}

	public boolean isDelete() {
		return this.commentStatus == CommentStatus.DELETE;
	}

	public void verifyVersion(int version) {
		if (this.version != version) {
			throw new VersionMismatchException(VERSION_MISMATCH);
		}
	}

	public boolean verifyAdmin(int currentAdminId) {
		return this.admin.getAdminId() == currentAdminId;
	}

	public void updateStatusDelete() {
		this.commentStatus = CommentStatus.DELETE;
	}

	public void modify(String commentText) {
		if (commentText != null && !commentText.isBlank()) {
			this.commentText = commentText;
		}
	}

}
