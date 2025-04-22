package edu.cit.Judify.TutoringSession;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.cit.Judify.TutoringSession.DTO.TutoringSessionDTO;
import edu.cit.Judify.TutoringSession.DTO.TutoringSessionDTOMapper;
import edu.cit.Judify.User.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/tutoring-sessions")
@CrossOrigin(origins = "*")
@Tag(name = "Tutoring Sessions", description = "Tutoring session management endpoints")
public class TutoringSessionController {

    private final TutoringSessionService sessionService;
    private final TutoringSessionDTOMapper sessionDTOMapper;

    @Autowired
    public TutoringSessionController(TutoringSessionService sessionService, 
                                    TutoringSessionDTOMapper sessionDTOMapper) {
        this.sessionService = sessionService;
        this.sessionDTOMapper = sessionDTOMapper;
    }

    @Operation(summary = "Create a new tutoring session", description = "Creates a new tutoring session between a tutor and a student")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Session successfully created",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = TutoringSessionDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping("/createSession")
    public ResponseEntity<TutoringSessionDTO> createSession(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Session data to create", required = true)
            @RequestBody TutoringSessionDTO sessionDTO,
            @Parameter(hidden = true) @org.springframework.security.core.annotation.AuthenticationPrincipal 
            org.springframework.security.core.userdetails.UserDetails userDetails) {
        
        // Log incoming data
        System.out.println("Creating session with DTO: " + sessionDTO);
        System.out.println("Authentication: " + (userDetails != null ? userDetails.getUsername() : "No authentication"));
        
        // Check if studentId is already set in the DTO
        if (sessionDTO.getStudentId() == null) {
            // Try to set from authentication if available
            if (userDetails != null) {
                // Extract the user ID from the authenticated user
                String username = userDetails.getUsername();
                System.out.println("Authenticated username: " + username);
                
                // Get the user by username and set as student ID
                UserEntity student = sessionService.findUserByUsername(username);
                System.out.println("Found student: " + (student != null ? student.getUserId() : "null"));
                
                if (student != null) {
                    sessionDTO.setStudentId(student.getUserId());
                    System.out.println("Set student ID in DTO: " + student.getUserId());
                } else {
                    System.out.println("Student not found for username: " + username);
                    return ResponseEntity.badRequest().body(null);
                }
            } else {
                System.out.println("No authentication found and no student ID provided in request.");
                return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                    .body(null);
            }
        } else {
            System.out.println("Student ID was provided in the request: " + sessionDTO.getStudentId());
        }
        
        try {
            // Validate that required fields are present
            if (sessionDTO.getStudentId() == null) {
                System.out.println("Student ID is missing");
                return ResponseEntity.badRequest().body(null);
            }
            
            if (sessionDTO.getTutorId() == null) {
                System.out.println("Tutor ID is missing");
                return ResponseEntity.badRequest().body(null);
            }
            
            TutoringSessionEntity session = sessionDTOMapper.toEntity(sessionDTO);
            System.out.println("Converted DTO to entity, student ID: " + (session.getStudent() != null ? session.getStudent().getUserId() : "null"));
            
            TutoringSessionEntity savedSession = sessionService.createSession(session);
            System.out.println("Session saved successfully with ID: " + savedSession.getSessionId());
            
            return ResponseEntity.ok(sessionDTOMapper.toDTO(savedSession));
        } catch (Exception e) {
            System.out.println("Error creating session: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }

    @Operation(summary = "Get session by ID", description = "Returns a tutoring session by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the session"),
        @ApiResponse(responseCode = "404", description = "Session not found")
    })
    @GetMapping("/findById/{id}")
    public ResponseEntity<TutoringSessionDTO> getSessionById(
            @Parameter(description = "Session ID") @PathVariable Long id) {
        return sessionService.getSessionById(id)
                .map(sessionDTOMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get sessions by tutor", description = "Returns all tutoring sessions for a specific tutor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the tutor's sessions")
    })
    @GetMapping("/findByTutor/{tutorId}")
    public ResponseEntity<List<TutoringSessionDTO>> getTutorSessions(
            @Parameter(description = "Tutor ID") @PathVariable("tutorId") Long tutorId) {
        // Find the tutor by ID
        UserEntity tutor = new UserEntity();
        tutor.setUserId(tutorId);

        return ResponseEntity.ok(sessionService.getTutorSessions(tutor)
                .stream()
                .map(sessionDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get sessions by student", description = "Returns all tutoring sessions for a specific student")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the student's sessions")
    })
    @GetMapping("/findByStudent/{studentId}")
    public ResponseEntity<List<TutoringSessionDTO>> getStudentSessions(
            @Parameter(description = "Student ID") @PathVariable("studentId") Long studentId) {
        // Find the student by ID
        UserEntity student = new UserEntity();
        student.setUserId(studentId);

        return ResponseEntity.ok(sessionService.getStudentSessions(student)
                .stream()
                .map(sessionDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get sessions by status", description = "Returns all tutoring sessions with a specific status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved sessions by status")
    })
    @GetMapping("/findByStatus/{status}")
    public ResponseEntity<List<TutoringSessionDTO>> getSessionsByStatus(
            @Parameter(description = "Session status (e.g., pending, confirmed, completed, cancelled)") 
            @PathVariable String status) {
        return ResponseEntity.ok(sessionService.getSessionsByStatus(status)
                .stream()
                .map(sessionDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get sessions by date range", description = "Returns all tutoring sessions within a specific date range")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved sessions within date range")
    })
    @GetMapping("/findByDateRange")
    public ResponseEntity<List<TutoringSessionDTO>> getSessionsBetweenDates(
            @Parameter(description = "Start date (yyyy-MM-dd)") 
            @RequestParam("start") @DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
            @Parameter(description = "End date (yyyy-MM-dd)") 
            @RequestParam("end") @DateTimeFormat(pattern = "yyyy-MM-dd") Date end) {
        return ResponseEntity.ok(sessionService.getSessionsBetweenDates(start, end)
                .stream()
                .map(sessionDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Update a tutoring session", description = "Updates an existing tutoring session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Session successfully updated"),
        @ApiResponse(responseCode = "404", description = "Session not found")
    })
    @PutMapping("/updateSession/{id}")
    public ResponseEntity<TutoringSessionDTO> updateSession(
            @Parameter(description = "Session ID") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated session data", required = true)
            @RequestBody TutoringSessionDTO sessionDTO) {
        TutoringSessionEntity sessionDetails = sessionDTOMapper.toEntity(sessionDTO);
        return ResponseEntity.ok(sessionDTOMapper.toDTO(
                sessionService.updateSession(id, sessionDetails)));
    }

    @Operation(summary = "Update session status", description = "Updates the status of an existing tutoring session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Session status successfully updated"),
        @ApiResponse(responseCode = "404", description = "Session not found")
    })
    @PutMapping("/updateStatus/{id}")
    public ResponseEntity<TutoringSessionDTO> updateSessionStatus(
            @Parameter(description = "Session ID") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "New session status", required = true)
            @RequestBody String status) {
        return ResponseEntity.ok(sessionDTOMapper.toDTO(
                sessionService.updateSessionStatus(id, status)));
    }

    @Operation(summary = "Delete a tutoring session", description = "Deletes a tutoring session by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Session successfully deleted"),
        @ApiResponse(responseCode = "404", description = "Session not found")
    })
    @DeleteMapping("/deleteSession/{id}")
    public ResponseEntity<Void> deleteSession(
            @Parameter(description = "Session ID") @PathVariable Long id) {
        sessionService.deleteSession(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get tutor sessions by status", description = "Returns all tutoring sessions for a specific tutor with a specific status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved tutor's sessions by status")
    })
    @GetMapping("/findByTutorAndStatus/{tutorId}/{status}")
    public ResponseEntity<List<TutoringSessionDTO>> getTutorSessionsByStatus(
            @Parameter(description = "Tutor ID") @PathVariable("tutorId") Long tutorId,
            @Parameter(description = "Session status") @PathVariable String status) {
        // Find the tutor by ID
        UserEntity tutor = new UserEntity();
        tutor.setUserId(tutorId);

        return ResponseEntity.ok(sessionService.getTutorSessionsByStatus(tutor, status)
                .stream()
                .map(sessionDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get student sessions by status", description = "Returns all tutoring sessions for a specific student with a specific status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved student's sessions by status")
    })
    @GetMapping("/findByStudentAndStatus/{studentId}/{status}")
    public ResponseEntity<List<TutoringSessionDTO>> getStudentSessionsByStatus(
            @Parameter(description = "Student ID") @PathVariable("studentId") Long studentId,
            @Parameter(description = "Session status") @PathVariable String status) {
        // Find the student by ID
        UserEntity student = new UserEntity();
        student.setUserId(studentId);

        return ResponseEntity.ok(sessionService.getStudentSessionsByStatus(student, status)
                .stream()
                .map(sessionDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get tutor sessions with pagination", description = "Returns a paginated list of tutoring sessions for a specific tutor with optional date filtering")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated tutor sessions")
    })
    @GetMapping("/findByTutorPaginated/{tutorId}")
    public ResponseEntity<Page<TutoringSessionDTO>> getTutorSessionsPaginated(
            @Parameter(description = "Tutor ID") @PathVariable("tutorId") Long tutorId,
            @Parameter(description = "Start date filter (optional)") 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @Parameter(description = "End date filter (optional)") 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        // Find the tutor by ID
        UserEntity tutor = new UserEntity();
        tutor.setUserId(tutorId);

        Page<TutoringSessionEntity> sessions = sessionService.getTutorSessionsPaginated(
                tutor, startDate, endDate, page, size);

        Page<TutoringSessionDTO> sessionDTOs = sessions.map(sessionDTOMapper::toDTO);
        return ResponseEntity.ok(sessionDTOs);
    }

    @Operation(summary = "Get student sessions with pagination", description = "Returns a paginated list of tutoring sessions for a specific student with optional date filtering")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated student sessions")
    })
    @GetMapping("/findByStudentPaginated/{studentId}")
    public ResponseEntity<Page<TutoringSessionDTO>> getStudentSessionsPaginated(
            @Parameter(description = "Student ID") @PathVariable("studentId") Long studentId,
            @Parameter(description = "Start date filter (optional)") 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @Parameter(description = "End date filter (optional)") 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        // Find the student by ID
        UserEntity student = new UserEntity();
        student.setUserId(studentId);

        Page<TutoringSessionEntity> sessions = sessionService.getStudentSessionsPaginated(
                student, startDate, endDate, page, size);

        Page<TutoringSessionDTO> sessionDTOs = sessions.map(sessionDTOMapper::toDTO);
        return ResponseEntity.ok(sessionDTOs);
    }

    @Operation(summary = "Get sessions by status with pagination", description = "Returns a paginated list of tutoring sessions with a specific status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated sessions by status")
    })
    @GetMapping("/findByStatusPaginated/{status}")
    public ResponseEntity<Page<TutoringSessionDTO>> getSessionsByStatusPaginated(
            @Parameter(description = "Session status") @PathVariable String status,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        Page<TutoringSessionEntity> sessions = sessionService.getSessionsByStatusPaginated(
                status, page, size);

        Page<TutoringSessionDTO> sessionDTOs = sessions.map(sessionDTOMapper::toDTO);
        return ResponseEntity.ok(sessionDTOs);
    }
} 
