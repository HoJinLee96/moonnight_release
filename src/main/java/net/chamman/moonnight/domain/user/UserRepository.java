package net.chamman.moonnight.domain.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import net.chamman.moonnight.domain.user.User.UserProvider;
import net.chamman.moonnight.domain.user.User.UserStatus;

@Component
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
	
	Optional<User> findByEmail(String email);
	
	Optional<User> findByUserProviderAndEmail(UserProvider userProvider, String email);

	Optional<User> findByPhone(String phone);

	Optional<User> findByUserProviderAndEmailAndPhone(UserProvider userProvider, String email, String phone);

	Optional<UserStatus> findUserStatusByUserProviderAndEmail(UserProvider userProvider, String email);

	boolean existsByUserProviderAndEmail(UserProvider userProvider, String email);

	boolean existsByUserProviderAndPhone(UserProvider userProvider, String phone);
	
	Optional<User> findByEmailAndUserStatus(String email, UserStatus userStatus);
	
	@Query("SELECT u FROM User u WHERE u.email = :email AND u.userStatus <> :userStatus")
	Optional<User> findByEmailAndUserStatusNot(@Param("email") String email,
			@Param("userStatus") UserStatus userStatus);
	
	@Query("SELECT u.userProvider FROM User u WHERE u.email = :email AND u.userStatus <> :userStatus")
	Optional<UserProvider> findUserProviderByEmailAndUserStatusNot(@Param("email") String email,
			@Param("userStatus") UserStatus userStatus);

	@Query("SELECT u.userProvider FROM User u WHERE u.phone = :phone AND u.userStatus <> :userStatus")
	Optional<UserProvider> findUserProviderByPhoneAndUserStatusNot(@Param("phone") String phone,
			@Param("userStatus") UserStatus userStatus);
	
	

}