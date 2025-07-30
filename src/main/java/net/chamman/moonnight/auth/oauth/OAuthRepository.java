package net.chamman.moonnight.auth.oauth;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import net.chamman.moonnight.auth.oauth.OAuth.OAuthProvider;
import net.chamman.moonnight.domain.user.User;

@Repository
public interface OAuthRepository extends JpaRepository<OAuth, Integer>{
	Optional<OAuth> findByOauthId(int oauthId);
	Optional<OAuth> findByOauthProviderAndOauthProviderId(OAuthProvider oauthProvider, String oauthProviderId);
	List<OAuth> findByUser(User user);
	
	@Query("SELECT a FROM OAuth a WHERE a.user.userId = :userId")
	List<OAuth> findByUserId(@Param("userId") int userId);

	
    @Transactional
    @Modifying
    @Query("DELETE FROM OAuth a WHERE a.user.userId = :userId")
    int deleteByUserId(@Param("userId") int userId);
}
