package edu.cit.Judify.Calendar;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.cit.Judify.TutorAvailability.TutorAvailabilityEntity;
import edu.cit.Judify.TutorAvailability.TutorAvailabilityService;
import edu.cit.Judify.TutoringSession.TutoringSessionEntity;
import edu.cit.Judify.TutoringSession.TutoringSessionService;
import edu.cit.Judify.User.UserEntity;
import edu.cit.Judify.User.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/calendar")
@CrossOrigin(origins = "*")
@Tag(name = "Google Calendar", description = "Google Calendar integration endpoints")
public class GoogleCalendarController {

    private final GoogleCalendarService calendarService;
    private final TutorAvailabilityService availabilityService;
    private final TutoringSessionService sessionService;
    private final UserRepository userRepository;

    @Autowired
    public GoogleCalendarController(GoogleCalendarService calendarService,
                                   TutorAvailabilityService availabilityService,
                                   TutoringSessionService sessionService,
                                   UserRepository userRepository) {
        this.calendarService = calendarService;
        this.availabilityService = availabilityService;
        this.sessionService = sessionService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Check calendar connection", description = "Checks if a user has connected their Google Calendar")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully checked connection status"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/check-connection")
    public ResponseEntity<Map<String, Boolean>> checkCalendarConnection(
            @Parameter(description = "User ID") @RequestParam Long userId) {
        
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        UserEntity user = userOpt.get();
        boolean isConnected = calendarService.isCalendarConnected(user);
        
        return ResponseEntity.ok(Map.of("connected", isConnected));
    }

    @Operation(summary = "Get available time slots", description = "Returns available time slots for a tutor on a specific date")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved available time slots"),
        @ApiResponse(responseCode = "404", description = "Tutor not found")
    })
    @GetMapping("/available-slots")
    public ResponseEntity<List<GoogleCalendarService.TimeSlot>> getAvailableTimeSlots(
            @Parameter(description = "Tutor ID") @RequestParam Long tutorId,
            @Parameter(description = "Date (YYYY-MM-DD)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Duration in minutes") @RequestParam(defaultValue = "60") int durationMinutes) {
        
        Optional<UserEntity> tutorOpt = userRepository.findById(tutorId);
        if (tutorOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        UserEntity tutor = tutorOpt.get();
        List<TutorAvailabilityEntity> availabilities = availabilityService.getTutorAvailability(tutor);
        
        List<GoogleCalendarService.TimeSlot> availableSlots = 
                calendarService.getAvailableTimeSlots(tutor, date, availabilities, durationMinutes);
        
        return ResponseEntity.ok(availableSlots);
    }

    @Operation(summary = "Check time slot availability", description = "Checks if a specific time slot is available for a tutor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully checked availability"),
        @ApiResponse(responseCode = "404", description = "Tutor not found")
    })
    @GetMapping("/check-availability")
    public ResponseEntity<Boolean> isTimeSlotAvailable(
            @Parameter(description = "Tutor ID") @RequestParam Long tutorId,
            @Parameter(description = "Date (YYYY-MM-DD)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Start time (HH:MM)") @RequestParam String startTime,
            @Parameter(description = "End time (HH:MM)") @RequestParam String endTime) {
        
        Optional<UserEntity> tutorOpt = userRepository.findById(tutorId);
        if (tutorOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        UserEntity tutor = tutorOpt.get();
        boolean isAvailable = calendarService.isTimeSlotAvailable(tutor, date, startTime, endTime);
        
        return ResponseEntity.ok(isAvailable);
    }

    @Operation(summary = "Create calendar event", description = "Creates a Google Calendar event for a tutoring session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully created calendar event"),
        @ApiResponse(responseCode = "404", description = "Session not found")
    })
    @PostMapping("/create-event")
    public ResponseEntity<Map<String, String>> createCalendarEvent(
            @Parameter(description = "Session ID") @RequestParam Long sessionId) {
        
        Optional<TutoringSessionEntity> sessionOpt = sessionService.getSessionById(sessionId);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        TutoringSessionEntity session = sessionOpt.get();
        String eventId = calendarService.createCalendarEvent(session);
        
        return ResponseEntity.ok(Map.of("eventId", eventId != null ? eventId : ""));
    }

    @Operation(summary = "Update calendar event", description = "Updates a Google Calendar event for a tutoring session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated calendar event"),
        @ApiResponse(responseCode = "404", description = "Session not found")
    })
    @PutMapping("/update-event")
    public ResponseEntity<Map<String, String>> updateCalendarEvent(
            @Parameter(description = "Session ID") @RequestParam Long sessionId,
            @Parameter(description = "Event ID") @RequestParam String eventId) {
        
        Optional<TutoringSessionEntity> sessionOpt = sessionService.getSessionById(sessionId);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        TutoringSessionEntity session = sessionOpt.get();
        String updatedEventId = calendarService.updateCalendarEvent(session, eventId);
        
        return ResponseEntity.ok(Map.of("eventId", updatedEventId != null ? updatedEventId : ""));
    }

    @Operation(summary = "Delete calendar event", description = "Deletes a Google Calendar event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully deleted calendar event"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/delete-event")
    public ResponseEntity<Map<String, Boolean>> deleteCalendarEvent(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Event ID") @RequestParam String eventId) {
        
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        UserEntity user = userOpt.get();
        boolean deleted = calendarService.deleteCalendarEvent(user.getUserId().toString(), eventId);
        
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }
}