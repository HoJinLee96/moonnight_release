package net.chamman.moonnight.domain.question;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import net.chamman.moonnight.domain.question.Question.QuestionStatus;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {

    @Query("SELECT q FROM Question q LEFT JOIN FETCH q.answers WHERE q.questionId = :questionId")
    Optional<Question> findByIdWithAnswers(int questionId);
    
    Page<Question> findAll(Pageable pageable);
    Page<Question> findByTitleContaining(String title, Pageable pageable);
    Page<Question> findAllByQuestionStatusNot(QuestionStatus status, Pageable pageable);
    Page<Question> findByTitleContainingAndQuestionStatusNot(String title, QuestionStatus status, Pageable pageable);
}
