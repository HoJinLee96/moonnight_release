package net.chamman.moonnight.domain.question;

import static net.chamman.moonnight.global.exception.HttpStatusCode.VERSION_MISMATCH;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.chamman.moonnight.domain.answer.Answer;
import net.chamman.moonnight.global.exception.VersionMismatchException;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "question")
public class Question {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "question_id")
	private int questionId;

	@Setter
	@Column(nullable = false)
	private String password;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, length = 1000)
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private QuestionStatus questionStatus;

	@Column(name = "client_ip", length = 50, nullable = false)
	private String clientIp;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = true)
	private LocalDateTime updatedAt;

	@Version
	private int version;

	// Question 하나는 여러 Answer를 가질 수 있음
	// cascade = CascadeType.ALL: 엔티티의 상태 변화(저장, 삭제 등)를 Answer 엔티티에도 전파시킵니다.
	// orphanRemoval = true: 부모(Question)와의 연관 관계가 끊어진 자식(Answer) 엔티티를 자동으로 삭제합니다.
	@OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<Answer> answers = new ArrayList<>();

	public enum QuestionStatus {
		PENDING("답변 대기 중"), ANSWER("답변 완료"), DELETE("삭제된 질문");

		private final String label;

		QuestionStatus(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}
	}

	@PrePersist
	public void onPrePersist() {
		this.createdAt = LocalDateTime.now();
	}

	// == 연관관계 편의 메서드 ==//
	public void addAnswer(Answer answer) {
		answers.add(answer);
		answer.setQuestion(this);
	}

	public void modify(String title, String content) {
		boolean modified = false;
		if (title != null && !title.isBlank()) {
			this.title = title;
			modified = true;
		}
		if (content != null && !content.isBlank()) {
			this.content = content;
			modified = true;
		}
		if (modified) {
			this.updatedAt = LocalDateTime.now();
		}
	}

	public void updateStatusDelete() {
		this.questionStatus = QuestionStatus.DELETE;
		this.updatedAt = LocalDateTime.now();
	}

	public void updateStatus(QuestionStatus questionStatus) {
		if (questionStatus != null) {
			this.questionStatus = questionStatus;
		}
	}

	public boolean isDelete() {
		return questionStatus == QuestionStatus.DELETE;
	}

	public void verifyVersion(int version) {
		if (this.version != version) {
			throw new VersionMismatchException(VERSION_MISMATCH);
		}
	}

	public void markAsAnswered() {
		this.questionStatus = QuestionStatus.ANSWER;
	}

	public void revertToPending() {
		this.questionStatus = QuestionStatus.PENDING;
	}

}