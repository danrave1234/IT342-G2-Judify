package edu.cit.Judify.TutorProfile;

import edu.cit.Judify.User.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TutorProfileRepository extends JpaRepository<TutorProfileEntity, Long> {
    TutorProfileEntity findByUser(UserEntity user);
    List<TutorProfileEntity> findBySubjectsExpertiseLike(String expertise);
    List<TutorProfileEntity> findByHourlyRateBetween(Double minRate, Double maxRate);
    List<TutorProfileEntity> findByAverageRatingGreaterThanEqual(Double rating);
} 