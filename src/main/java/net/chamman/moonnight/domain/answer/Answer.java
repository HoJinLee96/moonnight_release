package net.chamman.moonnight.domain.answer;

import static net.chamman.moonnight.global.exception.HttpStatusCode.VERSION_MISMATCH;

import java.time.LocalDateTime;

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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.chamman.moonnight.domain.admin.Admin;
import net.chamman.moonnight.domain.question.Question;
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
    private int answerId;

    // ManyToOne 관계: Answer(N) - Question(1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false, foreignKey = @ForeignKey(name = "FK_question_TO_answer"))
    @Setter
    private Question question;
    
    // ManyToOne 관계: Answer(N) - Admin(1)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "admin_id", nullable = false, foreignKey = @ForeignKey(name = "FK_admin_TO_answer"))
	@Setter
	private Admin admin;

    @Column(nullable = false, length = 2000)
    private String content;
    
	@Column(name = "client_ip", length = 50, nullable = false)
	private String clientIp;

	@Generated(event = EventType.INSERT)
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@Generated(event = EventType.UPDATE)
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

    @Version
    private int version;
    
    public void modify(String content) {
        if (content != null && !content.isBlank()) {
            this.content = content;
        }
    }

    public void verifyVersion(int version) {
    	if(this.version != version) {
            throw new VersionMismatchException(VERSION_MISMATCH);
    	}
    }
    
    public boolean verifyAdmin(int adminId) {
    	return this.admin.getAdminId() == adminId;
    }

}