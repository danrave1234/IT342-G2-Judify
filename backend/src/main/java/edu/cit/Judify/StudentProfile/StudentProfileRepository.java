package edu.cit.Judify.StudentProfile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfileEntity, Long> {
    
    /**
     * Find a student profile by user ID
     * @param userId The ID of the user
     * @return Optional containing the profile if found
     */
    @Query("SELECT sp FROM StudentProfileEntity sp WHERE sp.user.userId = :userId")
    Optional<StudentProfileEntity> findByUserId(@Param("userId") Long userId);
    
    /**
     * Check if a profile exists for a given user ID
     * @param userId The ID of the user
     * @return True if profile exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(sp) > 0 THEN true ELSE false END FROM StudentProfileEntity sp WHERE sp.user.userId = :userId")
    boolean existsByUserId(@Param("userId") Long userId);
} 