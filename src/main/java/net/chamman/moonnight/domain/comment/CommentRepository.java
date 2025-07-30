package net.chamman.moonnight.domain.comment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer>{
	List<Comment> findByEstimate_EstimateId(int estimateId);
	
    @Transactional
    @Modifying
    @Query("DELETE FROM Comment a WHERE a.user.userId = :userId")
    int deleteByUserId(@Param("userId") int userId);
}
