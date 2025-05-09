package edu.cit.Judify.TutoringSession;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cit.Judify.Email.EmailService;
import edu.cit.Judify.Notification.NotificationEntity;
import edu.cit.Judify.Notification.NotificationService;
import edu.cit.Judify.User.UserEntity;
import edu.cit.Judify.User.UserRepository;

@Service
public class TutoringSessionService {

    private static final Logger logger = LoggerFactory.getLogger(TutoringSessionService.class);

    private final TutoringSessionRepository sessionRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Autowired
    public TutoringSessionService(
            TutoringSessionRepository sessionRepository,
            EmailService emailService,
            NotificationService notificationService,
            UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @Transactional
    public TutoringSessionEntity createSession(TutoringSessionEntity session) {
        // Add validation logic here if needed
        TutoringSessionEntity savedSession = sessionRepository.save(session);

        // Create notifications for both tutor and student (this doesn't require email authentication)
        createSessionNotifications(savedSession);

        logger.info("Session created successfully with ID: {}", savedSession.getSessionId());

        // Email functionality disabled - uncomment if needed
        /*
        try {
            // Send confirmation emails with calendar attachments
            emailService.sendSessionConfirmationEmail(savedSession);
        } catch (MessagingException | IOException e) {
            // Log the error but don't prevent the session from being created
            logger.error("Failed to send session confirmation email", e);
        }
        */

        return savedSession;
    }

    /**
     * Creates in-app notifications for both tutor and student
     */
    private void createSessionNotifications(TutoringSessionEntity session) {
        // Create notification for tutor
        NotificationEntity tutorNotification = new NotificationEntity();
        tutorNotification.setUser(session.getTutor());
        tutorNotification.setType("session_scheduled");
        tutorNotification.setContent("New tutoring session scheduled with " + 
                session.getStudent().getFirstName() + " " + session.getStudent().getLastName() + 
                " on " + session.getStartTime());
        tutorNotification.setIsRead(false);

        // Create notification for student
        NotificationEntity studentNotification = new NotificationEntity();
        studentNotification.setUser(session.getStudent());
        studentNotification.setType("session_scheduled");
        studentNotification.setContent("Your tutoring session with " + 
                session.getTutor().getFirstName() + " " + session.getTutor().getLastName() + 
                " has been confirmed for " + session.getStartTime());
        studentNotification.setIsRead(false);

        // Save notifications
        notificationService.createNotification(tutorNotification);
        notificationService.createNotification(studentNotification);
    }

    public Optional<TutoringSessionEntity> getSessionById(Long id) {
        return sessionRepository.findById(id);
    }

    public Optional<TutoringSessionEntity> getTutoringSessionById(Long id) {
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

        // Store the old status for comparison
        String oldStatus = session.getStatus();

        // Update the status
        session.setStatus(status);
        TutoringSessionEntity updatedSession = sessionRepository.save(session);

        // Create notifications for status change
        createStatusChangeNotifications(updatedSession, oldStatus);

        logger.info("Session status updated: ID={}, Status={}", id, status);

        return updatedSession;
    }

    /**
     * Creates notifications for session status changes
     */
    private void createStatusChangeNotifications(TutoringSessionEntity session, String oldStatus) {
        String statusMessage = getStatusChangeMessage(session.getStatus(), oldStatus);
        if (statusMessage == null) {
            return; // No notification needed for this status change
        }

        // Create notification for tutor
        NotificationEntity tutorNotification = new NotificationEntity();
        tutorNotification.setUser(session.getTutor());
        tutorNotification.setType("session_status_changed");
        tutorNotification.setContent("Session with " + 
                session.getStudent().getFirstName() + " " + session.getStudent().getLastName() + 
                " " + statusMessage);
        tutorNotification.setIsRead(false);

        // Create notification for student
        NotificationEntity studentNotification = new NotificationEntity();
        studentNotification.setUser(session.getStudent());
        studentNotification.setType("session_status_changed");
        studentNotification.setContent("Session with " + 
                session.getTutor().getFirstName() + " " + session.getTutor().getLastName() + 
                " " + statusMessage);
        studentNotification.setIsRead(false);

        // Save notifications
        notificationService.createNotification(tutorNotification);
        notificationService.createNotification(studentNotification);
    }

    /**
     * Returns an appropriate message for the status change
     */
    private String getStatusChangeMessage(String newStatus, String oldStatus) {
        switch (newStatus.toUpperCase()) {
            case "COMPLETED":
                return "has been marked as completed.";
            case "CANCELLED":
                return "has been cancelled.";
            case "ONGOING":
                return "has started.";
            default:
                return null; // No notification for other status changes
        }
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

    /**
     * Find a user by their username
     * @param username The username to search for
     * @return The found user entity or null if not found
     */
    public UserEntity findUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    /**
     * Find sessions that overlap with the given time range for a specific tutor and have the specified status.
     * 
     * @param tutorId The ID of the tutor
     * @param startTime The start time of the range
     * @param endTime The end time of the range
     * @param status The status of the sessions to find
     * @return A list of overlapping sessions
     */
    public List<TutoringSessionEntity> findOverlappingSessionsByTutorAndStatus(Long tutorId, Date startTime, Date endTime, String status) {
        return sessionRepository.findOverlappingSessionsByTutorAndStatus(tutorId, startTime, endTime, status);
    }

    /**
     * Check if there are any approved sessions that overlap with the given time range for a specific tutor.
     * 
     * @param tutorId The ID of the tutor
     * @param startTime The start time of the range
     * @param endTime The end time of the range
     * @return true if there are overlapping approved sessions, false otherwise
     */
    public boolean hasOverlappingApprovedSessions(Long tutorId, Date startTime, Date endTime) {
        List<TutoringSessionEntity> overlappingSessions = findOverlappingSessionsByTutorAndStatus(
            tutorId, startTime, endTime, "APPROVED");
        return !overlappingSessions.isEmpty();
    }

    /**
     * Update a session with a conversation ID
     * 
     * @param sessionId The ID of the session to update
     * @param conversationEntity The conversation entity to associate with the session
     * @return The updated session entity
     */
    @Transactional
    public TutoringSessionEntity updateSessionWithConversation(Long sessionId, edu.cit.Judify.Conversation.ConversationEntity conversationEntity) {
        TutoringSessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with ID: " + sessionId));

        session.setConversation(conversationEntity);
        return sessionRepository.save(session);
    }

    /**
     * Find a session by conversation ID
     * 
     * @param conversationId The ID of the conversation
     * @return The session entity or null if not found
     */
    public TutoringSessionEntity getSessionByConversationId(Long conversationId) {
        return sessionRepository.findByConversationConversationId(conversationId);
    }
}
