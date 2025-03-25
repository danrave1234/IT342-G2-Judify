package edu.cit.Judify.TutoringSession;

import edu.cit.Judify.User.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Date;
import java.util.List;

@Repository
public interface TutoringSessionRepository extends JpaRepository<TutoringSessionEntity, Long> {
    List<TutoringSessionEntity> findByTutorOrderByStartTimeDesc(UserEntity tutor);
    List<TutoringSessionEntity> findByStudentOrderByStartTimeDesc(UserEntity student);
    List<TutoringSessionEntity> findByStatus(String status);
    List<TutoringSessionEntity> findByStartTimeBetween(Date start, Date end);
    List<TutoringSessionEntity> findByTutorAndStatus(UserEntity tutor, String status);
    List<TutoringSessionEntity> findByStudentAndStatus(UserEntity student, String status);
} 