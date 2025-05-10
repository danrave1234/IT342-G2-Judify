package edu.cit.Judify.TutoringSession;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.cit.Judify.User.UserEntity;

@Repository
public interface TutoringSessionRepository extends JpaRepository<TutoringSessionEntity, Long> {
    List<TutoringSessionEntity> findByTutorOrderByStartTimeDesc(UserEntity tutor);
    List<TutoringSessionEntity> findByStudentOrderByStartTimeDesc(UserEntity student);
    List<TutoringSessionEntity> findByStatus(String status);
    List<TutoringSessionEntity> findByStartTimeBetween(Date start, Date end);
    List<TutoringSessionEntity> findByTutorAndStatus(UserEntity tutor, String status);
    List<TutoringSessionEntity> findByStudentAndStatus(UserEntity student, String status);

    // Find session by conversation ID
    TutoringSessionEntity findByConversationConversationId(Long conversationId);

    Page<TutoringSessionEntity> findByTutor(UserEntity tutor, Pageable pageable);
    Page<TutoringSessionEntity> findByStudent(UserEntity student, Pageable pageable);
    Page<TutoringSessionEntity> findByStatus(String status, Pageable pageable);
    Page<TutoringSessionEntity> findByStartTimeBetween(Date start, Date end, Pageable pageable);
    Page<TutoringSessionEntity> findByTutorAndStartTimeBetween(UserEntity tutor, Date start, Date end, Pageable pageable);
    Page<TutoringSessionEntity> findByStudentAndStartTimeBetween(UserEntity student, Date start, Date end, Pageable pageable);
    Page<TutoringSessionEntity> findByTutorAndStatus(UserEntity tutor, String status, Pageable pageable);
    Page<TutoringSessionEntity> findByStudentAndStatus(UserEntity student, String status, Pageable pageable);

    /**
     * Find sessions that overlap with the given time range and have the specified status.
     * A session overlaps if:
     * - It starts before the end time and ends after the start time
     */
    @Query("SELECT s FROM TutoringSessionEntity s WHERE s.tutor.userId = :userId AND s.status = :status " +
           "AND s.startTime < :endTime AND s.endTime > :startTime")
    List<TutoringSessionEntity> findOverlappingSessionsByUserAndStatus(
            @Param("userId") Long userId,
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime,
            @Param("status") String status);
} 
