package edu.cit.Judify.TutoringSession;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cit.Judify.User.UserEntity;
import edu.cit.Judify.User.UserRepository;

@Service
public class TutoringSessionService {

    private final TutoringSessionRepository sessionRepository;
    private final UserRepository userRepository;

    @Autowired
    public TutoringSessionService(TutoringSessionRepository sessionRepository, UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Find a user by username
     * @param username the username to search for
     * @return the user entity if found, or null if not found
     */
    public UserEntity findUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    /**
     * Create a new tutoring session
     * @param session the session to create
     * @return the created session
     */
    @Transactional
    public TutoringSessionEntity createSession(TutoringSessionEntity session) {
        System.out.println("TutoringSessionService.createSession - Starting session creation");
        
        // Add validation logic here if needed
        // Ensure student is not null
        if (session.getStudent() == null) {
            System.out.println("TutoringSessionService.createSession - Student is null, throwing exception");
            throw new IllegalArgumentException("Student cannot be null when creating a tutoring session");
        }
        
        System.out.println("TutoringSessionService.createSession - Student ID: " + session.getStudent().getUserId());
        
        if (session.getTutor() == null) {
            System.out.println("TutoringSessionService.createSession - Tutor is null, throwing exception");
            throw new IllegalArgumentException("Tutor cannot be null when creating a tutoring session");
        }
        
        System.out.println("TutoringSessionService.createSession - Tutor ID: " + session.getTutor().getUserId());
        
        try {
            TutoringSessionEntity savedSession = sessionRepository.save(session);
            System.out.println("TutoringSessionService.createSession - Session saved successfully with ID: " + savedSession.getSessionId());
            return savedSession;
        } catch (Exception e) {
            System.out.println("TutoringSessionService.createSession - Error saving session: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to let @Transactional handle rollback
        }
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