package edu.cit.Judify.TutorProfile;

import edu.cit.Judify.User.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TutorProfileRepository extends JpaRepository<TutorProfileEntity, Long> {
    TutorProfileEntity findByUser(UserEntity user);
    List<TutorProfileEntity> findByExpertiseLike(String expertise);
    List<TutorProfileEntity> findByHourlyRateBetween(Double minRate, Double maxRate);
    List<TutorProfileEntity> findByRatingGreaterThanEqual(Double rating);
    Optional<TutorProfileEntity> findByUserUserId(Long userId);
    List<TutorProfileEntity> findBySubjectsContaining(String subject);
} 