package edu.cit.Judify.TutoringSession;

import java.util.Date;
import java.util.List;
import java.util.Optional;
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

import edu.cit.Judify.Conversation.ConversationEntity;
import edu.cit.Judify.Conversation.ConversationService;
import edu.cit.Judify.Message.MessageService;
import edu.cit.Judify.TutorProfile.TutorProfileService;
import edu.cit.Judify.TutoringSession.DTO.TutoringSessionDTO;
import edu.cit.Judify.TutoringSession.DTO.TutoringSessionDTOMapper;
import edu.cit.Judify.User.UserEntity;
import edu.cit.Judify.User.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import edu.cit.Judify.TutorProfile.TutorProfileEntity;

@RestController
@RequestMapping("/api/tutoring-sessions")
@CrossOrigin(origins = "*")
@Tag(name = "Tutoring Sessions", description = "Tutoring session management endpoints")
public class TutoringSessionController {

    private final TutoringSessionService sessionService;
    private final TutoringSessionDTOMapper sessionDTOMapper;
    private final ConversationService conversationService;
    private final MessageService messageService;
    private final UserRepository userRepository;

    @Autowired
    private TutorProfileService tutorProfileService;

    @Autowired
    public TutoringSessionController(TutoringSessionService sessionService, 
                                    TutoringSessionDTOMapper sessionDTOMapper,
                                    ConversationService conversationService,
                                    MessageService messageService,
                                    UserRepository userRepository) {
        this.sessionService = sessionService;
        this.sessionDTOMapper = sessionDTOMapper;
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.userRepository = userRepository;
    }

    /**
     * Helper method to ensure we always get a userId from either a userId or tutorProfileId
     * @param id The ID to check (could be either a userId or tutorProfileId)
     * @return The userId (either the original ID if it's a valid userId, or converted from tutorProfileId)
     */
    private Long ensureUserId(Long id) {
        try {
            // First check if this ID exists as a userId
            boolean userExists = userRepository.existsById(id);
            if (userExists) {
                System.out.println("ID " + id + " is already a valid userId");
                return id;  // It's already a userId
            }
            
            // If not, try to convert from tutorProfileId
            System.out.println("ID " + id + " is not a userId, trying to convert from tutorProfileId...");
            Long userId = tutorProfileService.getUserIdFromTutorId(id);
            System.out.println("Successfully converted tutorProfileId " + id + " to userId " + userId);
            return userId;
        } catch (Exception e) {
            System.out.println("ERROR converting ID: " + e.getMessage());
            e.printStackTrace();
            // Return original as fallback
            return id;
        }
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
                }
            } else {
                System.out.println("No authentication found. Student ID must be provided in request.");
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

            if (sessionDTO.getUserId() == null) {
                System.out.println("Tutor ID is missing");
                return ResponseEntity.badRequest().body(null);
            }
            
            // EARLY CONVERSION: Pre-process tutorId to ensure it's a valid userId BEFORE entity conversion
            // This ensures the conversation will be created with the proper userId
            Long initialTutorId = sessionDTO.getUserId();
            Long processedTutorId = null;
            
            try {
                // First check if this ID exists as a userId already
                boolean isUserIdValid = userRepository.existsById(initialTutorId);
                if (isUserIdValid) {
                    System.out.println("tutorId " + initialTutorId + " is already a valid userId, using directly");
                    processedTutorId = initialTutorId;
                } else {
                    // Only if it's not a valid userId, try to convert it from tutorProfileId
                    System.out.println("tutorId " + initialTutorId + " is not a userId, trying to convert from tutorProfileId...");
                    processedTutorId = tutorProfileService.getUserIdFromTutorId(initialTutorId);
                    System.out.println("Successfully mapped tutorProfileId " + initialTutorId + 
                                  " to userId " + processedTutorId + " BEFORE any operations");
                }
                
                // Always update the DTO to use the processed ID for all subsequent operations
                sessionDTO.setUserId(processedTutorId);
            } catch (Exception e) {
                // If this fails, it could mean either the ID is invalid
                System.out.println("Failed to pre-process tutorId: " + e.getMessage());
                return ResponseEntity.badRequest().body(null);
            }

            // Set initial status to PENDING for negotiation
            if (sessionDTO.getStatus() == null) {
                sessionDTO.setStatus("PENDING");
            }

            // Set initial acceptance status
            sessionDTO.setStudentAccepted(true); // Student initiates, so they accept by default
            sessionDTO.setTutorAccepted(false);  // Tutor needs to accept

            // Now convert to entity using the potentially modified DTO (with corrected tutorId)
            TutoringSessionEntity session = sessionDTOMapper.toEntity(sessionDTO);
            System.out.println("Converted DTO to entity, student ID: " + 
                             (session.getStudent() != null ? session.getStudent().getUserId() : "null") +
                             ", tutor ID: " + 
                             (session.getTutor() != null ? session.getTutor().getUserId() : "null"));

            // Create a conversation for the session with proper user entities
            Long studentUserId = session.getStudent().getUserId();
            
            // For tutor, we need to be especially careful to ensure we have a userId, not a tutorProfileId
            // The tutorId should now be a valid userId due to our early preprocessing
            Long tutorUserId = sessionDTO.getUserId();
            
            // Debug information
            System.out.println("Using preprocessed tutorId=" + tutorUserId + " for conversation creation");
            
            System.out.println("Creating conversation between student userId=" + studentUserId +
                             " and tutor userId=" + tutorUserId);
            
            // Verify the users from the repository to ensure we have full user entities
            UserEntity studentUser;
            UserEntity tutorUser;
            
            try {
                studentUser = userRepository.findById(studentUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Student user not found with ID: " + studentUserId));
                System.out.println("Found student user: " + studentUser.getUsername() + " (ID: " + studentUser.getUserId() + ")");
            } catch (Exception e) {
                System.out.println("Error finding student user: " + e.getMessage());
                e.printStackTrace();
                throw new IllegalArgumentException("Failed to find student user: " + e.getMessage());
            }
            
            try {
                // Always use our explicitly converted ID, not the one from the session entity
                tutorUser = userRepository.findById(tutorUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Tutor user not found with ID: " + tutorUserId));
                System.out.println("Found tutor user: " + tutorUser.getUsername() + " (ID: " + tutorUser.getUserId() + ")");
                
                // Safety check: If session's tutorId doesn't match our converted one, update it
                if (session.getTutor() == null || !session.getTutor().getUserId().equals(tutorUserId)) {
                    System.out.println("WARNING: Session's tutor doesn't match expected ID, updating...");
                    UserEntity tutor = userRepository.findById(tutorUserId)
                        .orElseThrow(() -> new RuntimeException("Tutor not found with ID: " + tutorUserId));
                    session.setTutor(tutor);
                }
            } catch (Exception e) {
                System.out.println("Error finding tutor user: " + e.getMessage());
                e.printStackTrace();
                throw new IllegalArgumentException("Failed to find tutor user: " + e.getMessage());
            }
            
            // Create the conversation through the service, which will check for existing conversations
            ConversationEntity savedConversation = null;
            try {
                // Create a new conversation for each tutoring session instead of finding existing ones
                // This fixes the unique constraint violation error
                ConversationEntity newConversation = new ConversationEntity();
                newConversation.setStudent(studentUser);
                newConversation.setTutor(tutorUser);
                savedConversation = conversationService.createConversation(newConversation);
                
                System.out.println("Created new conversation ID: " + savedConversation.getConversationId() +
                                  " between student=" + savedConversation.getStudent().getUserId() +
                                  " and tutor=" + savedConversation.getTutor().getUserId());
                
                // Final verification that we're using the right tutor user 
                if (!savedConversation.getTutor().getUserId().equals(tutorUserId)) {
                    System.out.println("WARNING: Conversation created with different tutor user ID: " + 
                                      savedConversation.getTutor().getUserId() + " instead of " + tutorUserId);
                }
            } catch (Exception e) {
                System.out.println("Error creating conversation: " + e.getMessage());
                e.printStackTrace();
                throw new IllegalArgumentException("Failed to create conversation: " + e.getMessage());
            }

            // Link the conversation to the session
            session.setConversation(savedConversation);

            // Save the session
            TutoringSessionEntity savedSession = sessionService.createSession(session);
            System.out.println("Session saved successfully with ID: " + savedSession.getSessionId());

            // Double-check conversion from tutorId to userId
            boolean isTutorIdSameAsUser = sessionDTO.getUserId().equals(session.getTutor().getUserId());
            if (!isTutorIdSameAsUser) {
                System.out.println("Note: Successfully converted tutorProfileId " + sessionDTO.getUserId() + 
                                 " to tutorUserId " + session.getTutor().getUserId() + " for session");
            }
            
            // Send a session details message in the conversation
            try {
                // For extra safety, use the tutor user ID from the conversation entity
                messageService.sendSessionDetailsMessage(
                    savedConversation.getConversationId(),
                    savedSession.getSessionId(),
                    session.getStudent().getUserId(),   // Student is the sender of the booking
                    savedConversation.getTutor().getUserId()  // Use conversation's tutor ID for consistency
                );
                System.out.println("Session details message sent in conversation: " + savedConversation.getConversationId());
            } catch (Exception e) {
                System.out.println("Error sending session details message: " + e.getMessage());
                e.printStackTrace();  // Print stack trace for better debugging
                // Continue even if message sending fails
            }

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

    @Operation(summary = "Get sessions by user", description = "Returns all tutoring sessions for a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the user's sessions")
    })
    @GetMapping("/findByUser/{userId}")
    public ResponseEntity<List<TutoringSessionDTO>> getUserSessions(
            @Parameter(description = "User ID") @PathVariable("userId") Long userId) {
        // Find the user by ID
        UserEntity user = new UserEntity();
        user.setUserId(userId);

        // Get all sessions where the user is either a tutor or student
        return ResponseEntity.ok(sessionService.getAllUserSessions(user)
                .stream()
                .map(sessionDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get sessions where user is tutor", description = "Returns all tutoring sessions where a specific user is the tutor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the tutor's sessions")
    })
    @GetMapping("/findByTutor/{tutorUserId}")
    public ResponseEntity<List<TutoringSessionDTO>> getTutorSessions(
            @Parameter(description = "Tutor User ID") @PathVariable("tutorUserId") Long tutorUserId) {
        // Find the user by ID
        UserEntity tutor = new UserEntity();
        tutor.setUserId(tutorUserId);

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

        // Get the sessions and convert to DTOs with tutor names included
        List<TutoringSessionDTO> sessionDTOs = sessionService.getStudentSessions(student)
                .stream()
                .map(sessionDTOMapper::toDTO)
                .collect(Collectors.toList());

        // Log for debugging
        System.out.println("Found " + sessionDTOs.size() + " sessions for student ID: " + studentId);
        for (TutoringSessionDTO session : sessionDTOs) {
            System.out.println("Session ID: " + session.getSessionId() + 
                              ", Tutor ID: " + session.getUserId() + 
                              ", Tutor Name: " + session.getTutorName());
        }

        return ResponseEntity.ok(sessionDTOs);
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

    @Operation(summary = "Get user sessions by status", description = "Returns all tutoring sessions for a specific user with a specific status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user's sessions by status")
    })
    @GetMapping("/findByUserAndStatus/{userId}/{status}")
    public ResponseEntity<List<TutoringSessionDTO>> getUserSessionsByStatus(
            @Parameter(description = "User ID") @PathVariable("userId") Long userId,
            @Parameter(description = "Session status") @PathVariable String status) {
        // Find the user by ID
        UserEntity tutor = new UserEntity();
        tutor.setUserId(userId);

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

    @Operation(summary = "Get user sessions with pagination", description = "Returns a paginated list of tutoring sessions for a specific user with optional date filtering")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated user sessions")
    })
    @GetMapping("/findByUserPaginated/{userId}")
    public ResponseEntity<Page<TutoringSessionDTO>> getUserSessionsPaginated(
            @Parameter(description = "User ID") @PathVariable("userId") Long userId,
            @Parameter(description = "Start date filter (optional)") 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @Parameter(description = "End date filter (optional)") 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        // Find the user by ID
        UserEntity tutor = new UserEntity();
        tutor.setUserId(userId);

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

        // Get the paginated sessions, with enhanced DTO including tutor names
        Page<TutoringSessionDTO> pagedSessions = sessionService.getStudentSessionsPaginated(
                student, startDate, endDate, page, size)
                .map(sessionDTOMapper::toDTO);

        // Debug logging
        System.out.println("Found " + pagedSessions.getTotalElements() + " total sessions for student " + 
                          studentId + " (page " + page + " of " + pagedSessions.getTotalPages() + ")");

        return ResponseEntity.ok(pagedSessions);
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

    @Operation(summary = "Check for overlapping approved sessions", 
               description = "Checks if there are any approved sessions that overlap with the given time range for a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Check completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters")
    })
    @GetMapping("/checkOverlap")
    public ResponseEntity<Boolean> checkOverlappingApprovedSessions(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Start time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startTime,
            @Parameter(description = "End time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endTime) {

        if (userId == null || startTime == null || endTime == null) {
            return ResponseEntity.badRequest().body(null);
        }

        boolean hasOverlap = sessionService.hasOverlappingApprovedSessions(userId, startTime, endTime);
        return ResponseEntity.ok(hasOverlap);
    }

    @Operation(summary = "Get session by conversation ID", description = "Returns a tutoring session associated with a specific conversation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the session"),
        @ApiResponse(responseCode = "404", description = "Session not found for the given conversation ID")
    })
    @GetMapping("/findByConversation/{conversationId}")
    public ResponseEntity<TutoringSessionDTO> getSessionByConversationId(
            @Parameter(description = "Conversation ID") @PathVariable Long conversationId) {
        try {
            System.out.println("Looking for session with conversation ID: " + conversationId);
            
            // Check if conversation exists
            boolean conversationExists = conversationService.getConversationById(conversationId).isPresent();
            if (!conversationExists) {
                System.out.println("Conversation not found with ID: " + conversationId);
                return ResponseEntity.notFound().build();
            }
            
            TutoringSessionEntity session = sessionService.getSessionByConversationId(conversationId);
            if (session == null) {
                System.out.println("No session found for conversation ID: " + conversationId);
                return ResponseEntity.notFound().build();
            }
            
            System.out.println("Found session: " + session.getSessionId() + " for conversation: " + conversationId);
            return ResponseEntity.ok(sessionDTOMapper.toDTO(session));
        } catch (Exception e) {
            System.out.println("Error retrieving session for conversation " + conversationId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Accept a tutoring session", description = "Tutor accepts a pending tutoring session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Session successfully accepted",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = TutoringSessionDTO.class))),
        @ApiResponse(responseCode = "404", description = "Session not found"),
        @ApiResponse(responseCode = "400", description = "Invalid operation")
    })
    @PutMapping("/acceptSession/{sessionId}")
    public ResponseEntity<TutoringSessionDTO> acceptSession(
            @Parameter(description = "Session ID") @PathVariable Long sessionId,
            @Parameter(hidden = true) @org.springframework.security.core.annotation.AuthenticationPrincipal 
            org.springframework.security.core.userdetails.UserDetails userDetails) {

        try {
            // Get the session
            TutoringSessionEntity session = sessionService.getSessionById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found"));

            // Verify the user is the tutor for this session
            if (userDetails != null) {
                String username = userDetails.getUsername();
                UserEntity user = sessionService.findUserByUsername(username);

                if (user == null || !user.getUserId().equals(session.getTutor().getUserId())) {
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                        .body(null);
                }
            }

            // Set tutor accepted to true
            session.setTutorAccepted(true);

            // If both parties have accepted, update status to SCHEDULED
            if (Boolean.TRUE.equals(session.getStudentAccepted()) && Boolean.TRUE.equals(session.getTutorAccepted())) {
                session.setStatus("SCHEDULED");
            }

            // Save the updated session
            TutoringSessionEntity updatedSession = sessionService.updateSession(sessionId, session);

            // Send a session action message in the conversation
            if (updatedSession.getConversation() != null) {
                try {
                    messageService.sendSessionActionMessage(
                        updatedSession.getConversation().getConversationId(),
                        updatedSession.getSessionId(),
                        updatedSession.getTutor().getUserId(),  // Tutor is the sender of the acceptance
                        updatedSession.getStudent().getUserId(), // Student is the receiver of the acceptance
                        true // Accepted
                    );
                    System.out.println("Session acceptance message sent in conversation: " + 
                                      updatedSession.getConversation().getConversationId());
                } catch (Exception e) {
                    System.out.println("Error sending session acceptance message: " + e.getMessage());
                    // Continue even if message sending fails
                }
            }

            return ResponseEntity.ok(sessionDTOMapper.toDTO(updatedSession));
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }

    @Operation(summary = "Reject a tutoring session", description = "Tutor rejects a pending tutoring session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Session successfully rejected",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = TutoringSessionDTO.class))),
        @ApiResponse(responseCode = "404", description = "Session not found"),
        @ApiResponse(responseCode = "400", description = "Invalid operation")
    })
    @PutMapping("/rejectSession/{sessionId}")
    public ResponseEntity<TutoringSessionDTO> rejectSession(
            @Parameter(description = "Session ID") @PathVariable Long sessionId,
            @Parameter(hidden = true) @org.springframework.security.core.annotation.AuthenticationPrincipal 
            org.springframework.security.core.userdetails.UserDetails userDetails) {

        try {
            // Get the session
            TutoringSessionEntity session = sessionService.getSessionById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found"));

            // Verify the user is the tutor for this session
            if (userDetails != null) {
                String username = userDetails.getUsername();
                UserEntity user = sessionService.findUserByUsername(username);

                if (user == null || !user.getUserId().equals(session.getTutor().getUserId())) {
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                        .body(null);
                }
            }

            // Set status to CANCELLED
            session.setStatus("CANCELLED");

            // Save the updated session
            TutoringSessionEntity updatedSession = sessionService.updateSession(sessionId, session);

            // Send a session action message in the conversation
            if (updatedSession.getConversation() != null) {
                try {
                    messageService.sendSessionActionMessage(
                        updatedSession.getConversation().getConversationId(),
                        updatedSession.getSessionId(),
                        updatedSession.getTutor().getUserId(),  // Tutor is the sender of the rejection
                        updatedSession.getStudent().getUserId(), // Student is the receiver of the rejection
                        false // Rejected
                    );
                    System.out.println("Session rejection message sent in conversation: " + 
                                      updatedSession.getConversation().getConversationId());
                } catch (Exception e) {
                    System.out.println("Error sending session rejection message: " + e.getMessage());
                    // Continue even if message sending fails
                }
            }

            return ResponseEntity.ok(sessionDTOMapper.toDTO(updatedSession));
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }

    @Operation(summary = "Update session details during negotiation", description = "Update price, location, or other details during the negotiation phase")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Session details successfully updated",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = TutoringSessionDTO.class))),
        @ApiResponse(responseCode = "404", description = "Session not found"),
        @ApiResponse(responseCode = "400", description = "Invalid operation")
    })
    @PutMapping("/negotiateSession/{sessionId}")
    public ResponseEntity<TutoringSessionDTO> negotiateSession(
            @Parameter(description = "Session ID") @PathVariable Long sessionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated session details", required = true)
            @RequestBody TutoringSessionDTO sessionDTO,
            @Parameter(hidden = true) @org.springframework.security.core.annotation.AuthenticationPrincipal 
            org.springframework.security.core.userdetails.UserDetails userDetails) {

        try {
            // Get the session
            TutoringSessionEntity session = sessionService.getSessionById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found"));

            // Verify the user is either the tutor or student for this session
            if (userDetails != null) {
                String username = userDetails.getUsername();
                UserEntity user = sessionService.findUserByUsername(username);

                if (user == null || 
                    (!user.getUserId().equals(session.getTutor().getUserId()) && 
                     !user.getUserId().equals(session.getStudent().getUserId()))) {
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                        .body(null);
                }

                // Determine if the user is the tutor or student
                boolean isUserTutor = user.getUserId().equals(session.getTutor().getUserId());

                // Update negotiable fields
                if (sessionDTO.getPrice() != null) {
                    session.setPrice(sessionDTO.getPrice());
                }

                if (sessionDTO.getLocationData() != null) {
                    session.setLocationData(sessionDTO.getLocationData());
                }

                if (sessionDTO.getMeetingLink() != null) {
                    session.setMeetingLink(sessionDTO.getMeetingLink());
                }

                if (sessionDTO.getNotes() != null) {
                    session.setNotes(sessionDTO.getNotes());
                }

                // Reset acceptance flags when details change
                session.setTutorAccepted(isUserTutor);
                session.setStudentAccepted(!isUserTutor);

                // Ensure status is NEGOTIATING
                session.setStatus("NEGOTIATING");

                // Save the updated session
                TutoringSessionEntity updatedSession = sessionService.updateSession(sessionId, session);

                return ResponseEntity.ok(sessionDTOMapper.toDTO(updatedSession));
            } else {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }

    /**
     * Create a tutoring session directly with tutorId in the request.
     * This is an alternative endpoint to help with frontend integration.
     */
    @PostMapping("/createWithTutor")
    public ResponseEntity<?> createSessionWithTutor(
            @RequestBody edu.cit.Judify.TutoringSession.DTO.TutoringSessionDTO sessionDTO,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        System.out.println("Creating session with tutorId directly: " + sessionDTO);
        
        try {
            // Validate the tutor ID exists in the request
            Long tutorId = sessionDTO.getUserId(); // Use userId as tutorId from frontend
            if (tutorId == null) {
                return ResponseEntity.badRequest().body("tutorId (as userId) is required");
            }
            
            // First check if this ID is already a valid user ID
            boolean isUserIdValid = userRepository.existsById(tutorId);
            if (isUserIdValid) {
                System.out.println("tutorId " + tutorId + " is a valid userId, using directly");
                // No need to convert, just proceed
            } else {
                // Only if it's not a valid userId, try to convert from tutorProfileId
                System.out.println("tutorId " + tutorId + " is not a userId, trying to convert from profile ID");
                
                // Get the tutor profile to find the associated userId
                Optional<TutorProfileEntity> tutorProfileOpt = tutorProfileService.findByUserId(tutorId);
                if (tutorProfileOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body("Tutor profile not found with ID: " + tutorId);
                }
                
                // Extract the tutor user ID
                TutorProfileEntity tutorProfile = tutorProfileOpt.get();
                Long tutorUserId = tutorProfile.getUser().getUserId();
                
                // Set the userId field for backend processing
                sessionDTO.setUserId(tutorUserId);
                System.out.println("Converted tutorProfileId " + tutorId + " to userId " + tutorUserId);
            }
            
            // Proceed with the regular session creation logic
            return createSessionInternal(sessionDTO, userDetails);
        } catch (Exception e) {
            System.out.println("Error creating session with tutorId: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error creating session: " + e.getMessage());
        }
    }
    
    // A helper method to handle the actual session creation
    private ResponseEntity<?> createSessionInternal(TutoringSessionDTO sessionDTO, UserDetails userDetails) {
        // Call the existing createSession method implementation
        return createSession(sessionDTO, userDetails);
    }
} 
