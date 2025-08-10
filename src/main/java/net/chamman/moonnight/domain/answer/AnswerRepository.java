package net.chamman.moonnight.domain.answer;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Integer> {

	List<Answer> findByQuestion_QuestionId(int questionId);
	
	@Query("SELECT a FROM Answer a JOIN FETCH a.admin WHERE a.answerId = :answerId")
    Optional<Answer> findByIdWithAdmin(@Param("answerId") int answerId);
	
    long countByQuestion_QuestionId(int questionId);
}
