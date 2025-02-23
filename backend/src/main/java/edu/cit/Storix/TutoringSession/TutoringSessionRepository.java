package edu.cit.Storix.TutoringSession;

import edu.cit.Storix.User.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Date;
import java.util.List;

@Repository
public interface TutoringSessionRepository extends JpaRepository<TutoringSessionEntity, Long> {
    List<TutoringSessionEntity> findByTutorOrderByScheduledStartDesc(UserEntity tutor);
    List<TutoringSessionEntity> findByLearnerOrderByScheduledStartDesc(UserEntity learner);
    List<TutoringSessionEntity> findByStatus(String status);
    List<TutoringSessionEntity> findByScheduledStartBetween(Date start, Date end);
    List<TutoringSessionEntity> findByTutorAndStatus(UserEntity tutor, String status);
    List<TutoringSessionEntity> findByLearnerAndStatus(UserEntity learner, String status);
} 