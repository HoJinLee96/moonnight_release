package net.chamman.moonnight.domain.estimate;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.chamman.moonnight.domain.estimate.Estimate.EstimateStatus;

@Repository
public interface EstimateRepository extends JpaRepository<Estimate, Integer> {
	
	@Query("SELECT e FROM Estimate e WHERE e.email = :recipient OR e.phone = :recipient")
	List<Estimate> findByEmailOrPhone(@Param("recipient") String recipient);
	
	@Query("SELECT e FROM Estimate e WHERE e.estimateId = :estimateId AND e.estimateStatus <> :estimateStatus")
	Optional<Estimate> findByIdAndEstimateStatusNot(@Param("estimateId") int estimateId,
			@Param("estimateStatus") EstimateStatus estimateStatus);
	
}
