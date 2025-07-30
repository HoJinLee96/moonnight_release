package net.chamman.moonnight.auth.sign.log;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import net.chamman.moonnight.auth.sign.log.SignLog.SignResult;

@Repository
public interface SignLogRepository extends JpaRepository<SignLog, Integer> {
	
	@Query("SELECT COUNT(l) FROM SignLog l WHERE l.id = :id AND l.provider = :provider AND l.resolveBy IS NULL AND l.signResult NOT IN :excludedResults")
	int countUnresolvedFailed(
			@Param("provider") String provider, 
			@Param("id") String id, 
			@Param("excludedResults") List<SignResult> excludedResults);
	
	@Query("SELECT COUNT(l) FROM SignLog l WHERE l.id = :id AND l.provider = :provider AND l.resolveBy IS NULL AND l.signResult IN :includedResults")
	int countUnresolvedWithResults(
			@Param("provider") String provider, 
			@Param("id") String id, 
			@Param("includedResults") List<SignResult> includedResults);
	
	@Transactional @Modifying
	@Query("UPDATE SignLog l SET l.resolveBy = :signLog WHERE l.id = :id AND l.resolveBy IS NULL AND l.signResult IN :includedResults")
	int resolveUnresolvedLogs(
			@Param("id") String id,
			@Param("signLog") SignLog signLog,
			@Param("includedResults") List<SignResult> includedResults
			);
}
