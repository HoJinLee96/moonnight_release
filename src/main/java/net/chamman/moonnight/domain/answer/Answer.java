package net.chamman.moonnight.domain.answer;

import static net.chamman.moonnight.global.exception.HttpStatusCode.QUESTION_PASSWORD_MISMATCH;
import static net.chamman.moonnight.global.exception.HttpStatusCode.VERSION_MISMATCH;

import java.time.LocalDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.chamman.moonnight.domain.question.Question;
import net.chamman.moonnight.domain.question.Question.QuestionStatus;
import net.chamman.moonnight.global.exception.MismatchPasswordException;
import net.chamman.moonnight.global.exception.VersionMismatchException;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "answer")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id")
    private int id;

    // ManyToOne 관계: Answer(N) - Question(1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @Setter // 연관관계 편의 메서드에서 사용하기 위해 추가
    private Question question;

    @Column(nullable = false, length = 2000)
    private String content;

	@Generated(event = EventType.INSERT)
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@Generated(event = EventType.UPDATE)
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

    @Version
    private int version;
    
    public void modify(String content) {
        if (content != null && content.isBlank()) {
            this.content = content;
        }
    }

    public void verifyVersion(int version) {
    	if(this.version != version) {
            throw new VersionMismatchException(VERSION_MISMATCH);
    	}
    }

}