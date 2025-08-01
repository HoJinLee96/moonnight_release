package net.chamman.moonnight.auth.adminSign.log;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import net.chamman.moonnight.auth.adminSign.log.AdminSignLog.SignResult;

@Repository
public interface AdminSignLogRepository extends JpaRepository<AdminSignLog, Integer> {
	
	@Query("SELECT COUNT(l) FROM AdminSignLog l WHERE l.admin.adminId = :adminId AND l.resolveBy IS NULL AND l.signResult NOT IN :excludedResults")
	int countUnresolvedFailed(
			@Param("adminId") String adminId, 
			@Param("excludedResults") List<SignResult> excludedResults);
	
	@Query("SELECT COUNT(l) FROM AdminSignLog l WHERE l.admin.adminId = :adminId AND l.resolveBy IS NULL AND l.signResult IN :includedResults")
	int countUnresolvedWithResults(
			@Param("adminId") String adminId, 
			@Param("includedResults") List<SignResult> includedResults);
	
	@Transactional @Modifying
	@Query("UPDATE AdminSignLog l SET l.resolveBy = :signLog WHERE l.admin.adminId = :adminId AND l.resolveBy IS NULL AND l.signResult IN :includedResults")
	int resolveUnresolvedLogs(
			@Param("adminId") String adminId,
			@Param("signLog") AdminSignLog signLog,
			@Param("includedResults") List<SignResult> includedResults
			);
}
