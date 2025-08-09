package net.chamman.moonnight.domain.answer;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Integer> {

	List<Answer> getByQuestionId(int questionId);

}
