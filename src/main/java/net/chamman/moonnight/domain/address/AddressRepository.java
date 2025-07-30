package net.chamman.moonnight.domain.address;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import net.chamman.moonnight.domain.user.User;

@Repository
public interface AddressRepository extends JpaRepository<Address, Integer>{
	
	List<Address> findByUser(User user);
	
	@Query("SELECT a FROM Address a WHERE a.user.userId = :userId ORDER BY a.isPrimary DESC, COALESCE(a.updatedAt, a.createdAt) DESC")
	List<Address> findByUserOrderByPrimaryAndDate(@Param("userId") int userId);
	
	// 해당 유저의 모든 주소 is_primary 값을 FALSE로 변경하는 메서드
	@Modifying
    @Transactional
	@Query("UPDATE Address a SET a.isPrimary = FALSE WHERE a.user.userId = :userId")
	int unsetPrimaryForUser(@Param("userId") int userId);
	
    @Transactional
    @Modifying
    @Query("DELETE FROM Address a WHERE a.user.userId = :userId")
    int deleteByUserId(@Param("userId") int userId);
}
