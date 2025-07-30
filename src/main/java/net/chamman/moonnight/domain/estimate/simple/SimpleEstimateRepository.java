package net.chamman.moonnight.domain.estimate.simple;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import io.lettuce.core.dynamic.annotation.Param;
import jakarta.transaction.Transactional;

@Repository
public interface SimpleEstimateRepository extends JpaRepository<SimpleEstimate, Integer>{
	List<SimpleEstimate> findByUser_UserId(int userId);
	List<SimpleEstimate> findByPhone(String phone);
	
    @Transactional
    @Modifying
    @Query("DELETE FROM SimpleEstimate a WHERE a.user.userId = :userId")
    int deleteByUserId(@Param("userId") int userId);
}
