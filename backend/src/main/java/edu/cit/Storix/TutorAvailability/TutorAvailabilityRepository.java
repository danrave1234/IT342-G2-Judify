package edu.cit.Storix.TutorAvailability;

import edu.cit.Storix.User.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TutorAvailabilityRepository extends JpaRepository<TutorAvailabilityEntity, Long> {
    List<TutorAvailabilityEntity> findByTutor(UserEntity tutor);
    List<TutorAvailabilityEntity> findByDayOfWeek(String dayOfWeek);
    List<TutorAvailabilityEntity> findByTutorAndDayOfWeek(UserEntity tutor, String dayOfWeek);
} 