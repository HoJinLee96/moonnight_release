package net.chamman.moonnight.domain.estimate;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import io.lettuce.core.dynamic.annotation.Param;
import jakarta.transaction.Transactional;

@Repository
public interface EstimateRepository extends JpaRepository<Estimate, Integer> {
	List<Estimate> findByUser_UserId(int userId);

	List<Estimate> findByPhone(String phone);

	/**
	 * 이메일 또는 전화번호가 주어진 수신자 정보와 일치하는 모든 견적을 조회합니다.
	 * 
	 * @param recipient 검색할 이메일 또는 전화번호
	 * @return 조건에 맞는 견적 리스트
	 */
	// 방법 1: @Query 사용 (가장 추천!)
	@Query("SELECT e FROM Estimate e WHERE e.email = :recipient OR e.phone = :recipient")
	List<Estimate> findByEmailOrPhone(@Param("recipient") String recipient);
	
    @Transactional
    @Modifying
    @Query("DELETE FROM Estimate a WHERE a.user.userId = :userId")
    int deleteByUserId(@Param("userId") int userId);
}
