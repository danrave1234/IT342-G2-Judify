package edu.cit.Judify.TutorProfile;

import edu.cit.Judify.User.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TutorProfileRepository extends JpaRepository<TutorProfileEntity, Long>, 
                                              JpaSpecificationExecutor<TutorProfileEntity> {
    TutorProfileEntity findByUser(UserEntity user);
    List<TutorProfileEntity> findByExpertiseLike(String expertise);
    List<TutorProfileEntity> findByHourlyRateBetween(Double minRate, Double maxRate);
    List<TutorProfileEntity> findByRatingGreaterThanEqual(Double rating);
    Optional<TutorProfileEntity> findByUserUserId(Long userId);
    List<TutorProfileEntity> findBySubjectsContaining(String subject);
    Page<TutorProfileEntity> findBySubjectsContaining(String subject, Pageable pageable);
} 