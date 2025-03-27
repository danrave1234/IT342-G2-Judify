package edu.cit.Judify.TutorSubject;

import edu.cit.Judify.TutorProfile.TutorProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TutorSubjectRepository extends JpaRepository<TutorSubjectEntity, Long> {
    List<TutorSubjectEntity> findByTutorProfile(TutorProfileEntity tutorProfile);
    List<TutorSubjectEntity> findByTutorProfileId(Long tutorProfileId);
    List<TutorSubjectEntity> findBySubjectContainingIgnoreCase(String subject);
    void deleteByTutorProfileId(Long tutorProfileId);
} 