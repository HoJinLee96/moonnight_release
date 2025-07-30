package net.chamman.moonnight.auth.verification;


import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationRepository extends JpaRepository<Verification, Integer>{

	
	// 최신 Verification 조회 (recipient 기준, 제일 최근에 요청한)
	Optional<Verification> findTopByRecipientOrderByCreatedAtDesc(String recipient);
	
	// 특정 verification_id가 인증되었는지 확인
	boolean existsByVerificationIdAndVerifyTrue(int verificationId);
	
	//  (recipient 기준, 10분 이내 요청한, 제일 최근에 요청한)
//  @Query("SELECT v FROM Verification v " +
//      "WHERE v.recipient = :recipient " +
//      "AND v.createdAt >= CURRENT_TIMESTAMP - INTERVAL 10 MINUTE" +
//      "ORDER BY v.createdAt DESC " +
//      "LIMIT 1")
//  Optional<Verification> findRecentVerificationWithin10Min(@Param("recipient") String recipient);
	
	// verify 상태 및 verify_at 업데이트, 수정 이전 flush, 수정 이후 영속성 컨텍스트 clear
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = "UPDATE verification SET verify = TRUE, verify_at = NOW() WHERE verification_id = :verId", nativeQuery = true)
	void markAsVerified(@Param("verId") int verId);
	
	//  (recipient 기준, 10분 이내 요청한, 제일 최근에 요청한)
	@Query(
			value = "SELECT * FROM verification " +
					"WHERE `recipient` = :recipient " +
					"AND created_at >= CURRENT_TIMESTAMP - INTERVAL 10 MINUTE " +
					"ORDER BY created_at DESC " +
					"LIMIT 1",
					nativeQuery = true
			)
	Optional<Verification> findRecentVerificationWithin10Min(@Param("recipient") String recipient);
	
	// 인증 요청이 최근 3분 이내인지 확인
	@Query(
			value = "SELECT EXISTS(SELECT 1 FROM verification WHERE verification_id = :verId AND created_at >= CURRENT_TIMESTAMP - INTERVAL 3 MINUTE)",
			nativeQuery = true
			)
	Long isWithinVerificationTime(@Param("verId") int verId);
	
//	@Query(value = """
//		    SELECT v.*, 
//		           CASE 
//		             WHEN TIMESTAMPDIFF(SECOND, v.created_at, NOW()) <= 180 THEN TRUE 
//		             ELSE FALSE 
//		           END AS is_valid 
//		    FROM verification v 
//		    WHERE v.verification_id = :verificationId 
//		""", nativeQuery = true)
//	Optional<Object[]> findVerificationWithValidity(@Param("verificationId") int verificationId);
	
	@Query(value = """
		    SELECT 
		        v.verification_id as verificationId,
		        v.client_ip as clientIp,
		        v.recipient as recipient,
		        v.verification_code as verificationCode,
		        v.send_status as sendStatus,
		        v.created_at as createdAt,
		        v.verify as verify,
		        v.verify_at as verifyAt,
		        CASE 
		            WHEN TIMESTAMPDIFF(SECOND, v.created_at, NOW()) <= 180 THEN 1 
		            ELSE 0 
		        END AS isValid
		    FROM verification v
		    WHERE v.verification_id = :verificationId
		    """, nativeQuery = true)
		Optional<VerificationProjection> findVerificationWithValidity(@Param("verificationId") int verificationId);

	
	public interface VerificationProjection {
	    Integer getVerificationId();
	    String getRequestIp();
	    String getRecipient();
	    String getVerificationCode();
	    Integer getSendStatus();
	    LocalDateTime getCreatedAt();
	    Boolean getVerify();
	    LocalDateTime getVerifyAt();
	    Integer getIsValid(); // alias 명 그대로!
	    default boolean isValid() {
	        return getIsValid() != null && getIsValid() == 1;
	    }
	}
	
}


