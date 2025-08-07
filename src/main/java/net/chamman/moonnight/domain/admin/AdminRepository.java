package net.chamman.moonnight.domain.admin;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import net.chamman.moonnight.domain.admin.Admin.AdminStatus;

@Component
@Repository
public interface AdminRepository extends JpaRepository<Admin, Integer> {
	
	Optional<Admin> findByEmail(String email);
	
	Optional<Admin> findByPhone(String phone);

	Optional<Admin> findByEmailAndPhone(String email, String phone);

	boolean existsByEmail(String email);

	boolean existsByPhone(String phone);
	
	Optional<Admin> findByEmailAndAdminStatus(String email, AdminStatus adminStatus);
	
	@Query("SELECT u FROM Admin u WHERE u.email = :email AND u.adminStatus <> :adminStatus")
	Optional<Admin> findByEmailAndAdminStatusNot(@Param("email") String email,
			@Param("adminStatus") AdminStatus adminStatus);
	
	@Query("SELECT u FROM Admin u WHERE u.phone = :phone AND u.adminStatus <> :adminStatus")
	Optional<Admin> findByPhoneAndAdminStatusNot(@Param("phone") String phone,
			@Param("adminStatus") AdminStatus adminStatus);
	
}