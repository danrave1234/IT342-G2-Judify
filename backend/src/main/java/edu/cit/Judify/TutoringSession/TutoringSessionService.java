package edu.cit.Judify.TutoringSession;

import edu.cit.Judify.User.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
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
        return sessionRepository.findByTutorOrderByScheduledStartDesc(tutor);
    }

    public List<TutoringSessionEntity> getLearnerSessions(UserEntity learner) {
        return sessionRepository.findByLearnerOrderByScheduledStartDesc(learner);
    }

    public List<TutoringSessionEntity> getSessionsByStatus(String status) {
        return sessionRepository.findByStatus(status);
    }

    public List<TutoringSessionEntity> getSessionsBetweenDates(Date start, Date end) {
        return sessionRepository.findByScheduledStartBetween(start, end);
    }

    @Transactional
    public TutoringSessionEntity updateSession(Long id, TutoringSessionEntity sessionDetails) {
        TutoringSessionEntity session = sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setScheduledStart(sessionDetails.getScheduledStart());
        session.setScheduledEnd(sessionDetails.getScheduledEnd());
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

    public List<TutoringSessionEntity> getLearnerSessionsByStatus(UserEntity learner, String status) {
        return sessionRepository.findByLearnerAndStatus(learner, status);
    }
} 