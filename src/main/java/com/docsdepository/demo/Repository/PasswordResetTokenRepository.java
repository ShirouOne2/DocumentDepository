package com.docsdepository.demo.Repository;

import com.docsdepository.demo.Entity.PasswordResetToken;
import com.docsdepository.demo.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {
    
    Optional<PasswordResetToken> findByTokenAndIsUsedFalse(String token);
    
    Optional<PasswordResetToken> findByUserAndIsUsedFalseAndExpiryTimeAfter(
        Users user, 
        LocalDateTime currentTime
    );
    
    void deleteByExpiryTimeBefore(LocalDateTime currentTime);
    
    void deleteByUser(Users user);
}