package edu.cit.Storix.TutoringSession;

import edu.cit.Storix.User.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/tutoring-sessions")
@CrossOrigin(origins = "*")
public class TutoringSessionController {

    private final TutoringSessionService sessionService;

    @Autowired
    public TutoringSessionController(TutoringSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<TutoringSessionEntity> createSession(@RequestBody TutoringSessionEntity session) {
        return ResponseEntity.ok(sessionService.createSession(session));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TutoringSessionEntity> getSessionById(@PathVariable Long id) {
        return sessionService.getSessionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tutor/{tutorId}")
    public ResponseEntity<List<TutoringSessionEntity>> getTutorSessions(@PathVariable UserEntity tutor) {
        return ResponseEntity.ok(sessionService.getTutorSessions(tutor));
    }

    @GetMapping("/learner/{learnerId}")
    public ResponseEntity<List<TutoringSessionEntity>> getLearnerSessions(@PathVariable UserEntity learner) {
        return ResponseEntity.ok(sessionService.getLearnerSessions(learner));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<TutoringSessionEntity>> getSessionsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(sessionService.getSessionsByStatus(status));
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<TutoringSessionEntity>> getSessionsBetweenDates(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date end) {
        return ResponseEntity.ok(sessionService.getSessionsBetweenDates(start, end));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TutoringSessionEntity> updateSession(
            @PathVariable Long id,
            @RequestBody TutoringSessionEntity sessionDetails) {
        return ResponseEntity.ok(sessionService.updateSession(id, sessionDetails));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<TutoringSessionEntity> updateSessionStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(sessionService.updateSessionStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        sessionService.deleteSession(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tutor/{tutorId}/status/{status}")
    public ResponseEntity<List<TutoringSessionEntity>> getTutorSessionsByStatus(
            @PathVariable UserEntity tutor,
            @PathVariable String status) {
        return ResponseEntity.ok(sessionService.getTutorSessionsByStatus(tutor, status));
    }

    @GetMapping("/learner/{learnerId}/status/{status}")
    public ResponseEntity<List<TutoringSessionEntity>> getLearnerSessionsByStatus(
            @PathVariable UserEntity learner,
            @PathVariable String status) {
        return ResponseEntity.ok(sessionService.getLearnerSessionsByStatus(learner, status));
    }
} 