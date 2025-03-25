package edu.cit.Judify.TutoringSession;

import edu.cit.Judify.User.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class TutoringSessionService {

    private final TutoringSessionRepository sessionRepository;

    @Autowired
    public TutoringSessionService(TutoringSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public TutoringSessionEntity createSession(TutoringSessionEntity session) {
        // Add validation logic here if needed
        return sessionRepository.save(session);
    }

    public Optional<TutoringSessionEntity> getSessionById(Long id) {
        return sessionRepository.findById(id);
    }

    public List<TutoringSessionEntity> getTutorSessions(UserEntity tutor) {
        return sessionRepository.findByTutorOrderByStartTimeDesc(tutor);
    }

    public List<TutoringSessionEntity> getStudentSessions(UserEntity student) {
        return sessionRepository.findByStudentOrderByStartTimeDesc(student);
    }

    public List<TutoringSessionEntity> getSessionsByStatus(String status) {
        return sessionRepository.findByStatus(status);
    }

    public List<TutoringSessionEntity> getSessionsBetweenDates(Date start, Date end) {
        return sessionRepository.findByStartTimeBetween(start, end);
    }

    @Transactional
    public TutoringSessionEntity updateSession(Long id, TutoringSessionEntity sessionDetails) {
        TutoringSessionEntity session = sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setStatus(sessionDetails.getStatus());
        session.setMeetingLink(sessionDetails.getMeetingLink());
        session.setLocationData(sessionDetails.getLocationData());

        return sessionRepository.save(session);
    }

    @Transactional
    public TutoringSessionEntity updateSessionStatus(Long id, String status) {
        TutoringSessionEntity session = sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        
        session.setStatus(status);
        return sessionRepository.save(session);
    }

    @Transactional
    public void deleteSession(Long id) {
        sessionRepository.deleteById(id);
    }

    public List<TutoringSessionEntity> getTutorSessionsByStatus(UserEntity tutor, String status) {
        return sessionRepository.findByTutorAndStatus(tutor, status);
    }

    public List<TutoringSessionEntity> getStudentSessionsByStatus(UserEntity student, String status) {
        return sessionRepository.findByStudentAndStatus(student, status);
    }

    // Paginated version of getTutorSessions with date range filter
    public Page<TutoringSessionEntity> getTutorSessionsPaginated(
            UserEntity tutor, Date startDate, Date endDate, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());
        
        // If date range is provided, filter by date range
        if (startDate != null && endDate != null) {
            return sessionRepository.findByTutorAndStartTimeBetween(
                    tutor, startDate, endDate, pageable);
        }
        
        return sessionRepository.findByTutor(tutor, pageable);
    }
    
    // Paginated version of getStudentSessions with date range filter
    public Page<TutoringSessionEntity> getStudentSessionsPaginated(
            UserEntity student, Date startDate, Date endDate, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());
        
        // If date range is provided, filter by date range
        if (startDate != null && endDate != null) {
            return sessionRepository.findByStudentAndStartTimeBetween(
                    student, startDate, endDate, pageable);
        }
        
        return sessionRepository.findByStudent(student, pageable);
    }
    
    // Paginated version of getSessionsByStatus
    public Page<TutoringSessionEntity> getSessionsByStatusPaginated(
            String status, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());
        return sessionRepository.findByStatus(status, pageable);
    }
} 