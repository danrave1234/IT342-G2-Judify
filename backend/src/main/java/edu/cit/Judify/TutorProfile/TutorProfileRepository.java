package edu.cit.Judify.TutorProfile;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.cit.Judify.User.UserEntity;

@Repository
public interface TutorProfileRepository extends JpaRepository<TutorProfileEntity, Long>, 
                                              JpaSpecificationExecutor<TutorProfileEntity> {
    TutorProfileEntity findByUser(UserEntity user);
    List<TutorProfileEntity> findByExpertiseLike(String expertise);
    List<TutorProfileEntity> findByHourlyRateBetween(Double minRate, Double maxRate);
    List<TutorProfileEntity> findByRatingGreaterThanEqual(Double rating);
    Optional<TutorProfileEntity> findByUserUserId(Long userId);
    
    // Custom JPQL query to find profiles by subject name from the TutorSubjects
    @Query("SELECT DISTINCT tp FROM TutorProfileEntity tp JOIN tp.subjectEntities ts WHERE LOWER(ts.subject) LIKE LOWER(CONCAT('%', :subject, '%'))")
    List<TutorProfileEntity> findBySubjectName(@Param("subject") String subject);
    
    // Custom JPQL query for paginated search by subject name
    @Query("SELECT DISTINCT tp FROM TutorProfileEntity tp JOIN tp.subjectEntities ts WHERE LOWER(ts.subject) LIKE LOWER(CONCAT('%', :subject, '%'))")
    Page<TutorProfileEntity> findBySubjectName(@Param("subject") String subject, Pageable pageable);
} 