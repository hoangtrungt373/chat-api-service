package vn.ttg.roadmap.chatapi.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.ttg.roadmap.chatapi.userservice.entity.User;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByUserUuid(String userUuid);
    
    Optional<User> findByProviderAndProviderId(User.Provider provider, String providerId);
    
    boolean existsByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByUserUuid(String userUuid);
    
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.userId = :userId")
    void updateStatus(@Param("userId") Integer userId, @Param("status") User.UserStatus status);
}
