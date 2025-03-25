package edu.cit.Judify.TutoringSession;

import edu.cit.Judify.TutoringSession.DTO.TutoringSessionDTO;
import edu.cit.Judify.TutoringSession.DTO.TutoringSessionDTOMapper;
import edu.cit.Judify.User.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tutoring-sessions")
@CrossOrigin(origins = "*")
public class TutoringSessionController {

    private final TutoringSessionService sessionService;
    private final TutoringSessionDTOMapper sessionDTOMapper;

    @Autowired
    public TutoringSessionController(TutoringSessionService sessionService, 
                                    TutoringSessionDTOMapper sessionDTOMapper) {
        this.sessionService = sessionService;
        this.sessionDTOMapper = sessionDTOMapper;
    }

    @PostMapping("/createSession")
    public ResponseEntity<TutoringSessionDTO> createSession(@RequestBody TutoringSessionDTO sessionDTO) {
        TutoringSessionEntity session = sessionDTOMapper.toEntity(sessionDTO);
        return ResponseEntity.ok(sessionDTOMapper.toDTO(sessionService.createSession(session)));
    }

    @GetMapping("/findById/{id}")
    public ResponseEntity<TutoringSessionDTO> getSessionById(@PathVariable Long id) {
        return sessionService.getSessionById(id)
                .map(sessionDTOMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/findByTutor/{tutorId}")
    public ResponseEntity<List<TutoringSessionDTO>> getTutorSessions(@PathVariable UserEntity tutor) {
        return ResponseEntity.ok(sessionService.getTutorSessions(tutor)
                .stream()
                .map(sessionDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/findByStudent/{studentId}")
    public ResponseEntity<List<TutoringSessionDTO>> getStudentSessions(@PathVariable UserEntity student) {
        return ResponseEntity.ok(sessionService.getStudentSessions(student)
                .stream()
                .map(sessionDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/findByStatus/{status}")
    public ResponseEntity<List<TutoringSessionDTO>> getSessionsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(sessionService.getSessionsByStatus(status)
                .stream()
                .map(sessionDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/findByDateRange")
    public ResponseEntity<List<TutoringSessionDTO>> getSessionsBetweenDates(
            @RequestParam("start") @DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
            @RequestParam("end") @DateTimeFormat(pattern = "yyyy-MM-dd") Date end) {
        return ResponseEntity.ok(sessionService.getSessionsBetweenDates(start, end)
                .stream()
                .map(sessionDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @PutMapping("/updateSession/{id}")
    public ResponseEntity<TutoringSessionDTO> updateSession(
            @PathVariable Long id,
            @RequestBody TutoringSessionDTO sessionDTO) {
        TutoringSessionEntity sessionDetails = sessionDTOMapper.toEntity(sessionDTO);
        return ResponseEntity.ok(sessionDTOMapper.toDTO(
                sessionService.updateSession(id, sessionDetails)));
    }

    @PutMapping("/updateStatus/{id}")
    public ResponseEntity<TutoringSessionDTO> updateSessionStatus(
            @PathVariable Long id,
            @RequestBody String status) {
        return ResponseEntity.ok(sessionDTOMapper.toDTO(
                sessionService.updateSessionStatus(id, status)));
    }

    @DeleteMapping("/deleteSession/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        sessionService.deleteSession(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/findByTutorAndStatus/{tutorId}/{status}")
    public ResponseEntity<List<TutoringSessionDTO>> getTutorSessionsByStatus(
            @PathVariable UserEntity tutor,
            @PathVariable String status) {
        return ResponseEntity.ok(sessionService.getTutorSessionsByStatus(tutor, status)
                .stream()
                .map(sessionDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/findByStudentAndStatus/{studentId}/{status}")
    public ResponseEntity<List<TutoringSessionDTO>> getStudentSessionsByStatus(
            @PathVariable UserEntity student,
            @PathVariable String status) {
        return ResponseEntity.ok(sessionService.getStudentSessionsByStatus(student, status)
                .stream()
                .map(sessionDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }
} 