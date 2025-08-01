package net.chamman.moonnight.domain.comment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer>{
	List<Comment> findByEstimate_EstimateId(int estimateId);
	void deleteByEstimate_EstimateId(int estimateId);
}
