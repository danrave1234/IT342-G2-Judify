package edu.cit.Judify.Conversation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.cit.Judify.Conversation.DTO.ConversationDTO;
import edu.cit.Judify.Conversation.DTO.ConversationDTOMapper;
import edu.cit.Judify.TutorProfile.TutorProfileService;
import edu.cit.Judify.User.UserEntity;
import edu.cit.Judify.User.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/conversations")
@CrossOrigin(origins = "*")
@Tag(name = "Conversations", description = "Conversation management endpoints")
public class ConversationController {

    private final ConversationService conversationService;
    private final ConversationDTOMapper conversationDTOMapper;
    private final UserService userService;
    private final TutorProfileService tutorProfileService;

    @Autowired
    public ConversationController(ConversationService conversationService, ConversationDTOMapper conversationDTOMapper, UserService userService, TutorProfileService tutorProfileService) {
        this.conversationService = conversationService;
        this.conversationDTOMapper = conversationDTOMapper;
        this.userService = userService;
        this.tutorProfileService = tutorProfileService;
    }

    @Operation(summary = "Create a new conversation or find existing", description = "Creates a new conversation between two users or returns an existing one")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Conversation successfully created or found",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConversationDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping("/createConversation")
    public ResponseEntity<ConversationDTO> createConversation(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Conversation data to create", required = true)
            @RequestBody ConversationDTO conversationDTO) {
        try {
            // Get user entities
            UserEntity student = userService.getUserById(conversationDTO.getUser1Id())
                    .orElseThrow(() -> new RuntimeException("Student not found with ID: " + conversationDTO.getUser1Id()));
            UserEntity tutor = userService.getUserById(conversationDTO.getUser2Id())
                    .orElseThrow(() -> new RuntimeException("Tutor not found with ID: " + conversationDTO.getUser2Id()));

            // Find or create the conversation
            ConversationEntity savedConversation = conversationService.findOrCreateConversation(student, tutor);

            return ResponseEntity.ok(conversationDTOMapper.toDTO(savedConversation));
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }

    @Operation(summary = "Find conversation between two users", description = "Finds or creates a conversation between two specific users")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Conversation successfully found or created",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConversationDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @GetMapping("/findBetweenUsers/{userId1}/{userId2}")
    public ResponseEntity<ConversationDTO> findConversationBetweenUsers(
            @Parameter(description = "First user ID") @PathVariable Long userId1,
            @Parameter(description = "Second user ID") @PathVariable Long userId2) {
        try {
            // Get user entities
            UserEntity student = userService.getUserById(userId1)
                    .orElseThrow(() -> new RuntimeException("Student not found with ID: " + userId1));
            UserEntity tutor = userService.getUserById(userId2)
                    .orElseThrow(() -> new RuntimeException("Tutor not found with ID: " + userId2));

            // Find or create the conversation
            ConversationEntity conversation = conversationService.findOrCreateConversation(student, tutor);

            return ResponseEntity.ok(conversationDTOMapper.toDTO(conversation));
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }

    @Operation(summary = "Get conversation by ID", description = "Returns a conversation by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the conversation"),
        @ApiResponse(responseCode = "404", description = "Conversation not found")
    })
    @GetMapping("/findById/{id}")
    public ResponseEntity<ConversationDTO> getConversationById(
            @Parameter(description = "Conversation ID") @PathVariable Long id) {
        return conversationService.getConversationById(id)
                .map(conversationDTOMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get user's conversations", description = "Returns all conversations for a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user's conversations")
    })
    @GetMapping("/findByUser/{userId}")
    public ResponseEntity<List<ConversationDTO>> getUserConversations(
            @Parameter(description = "User ID") @PathVariable("userId") Long userId) {
        UserEntity user = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        return ResponseEntity.ok(conversationService.getUserConversations(user)
                .stream()
                .map(conversationDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Delete a conversation", description = "Deletes a conversation by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Conversation successfully deleted"),
        @ApiResponse(responseCode = "404", description = "Conversation not found")
    })
    @DeleteMapping("/deleteConversation/{id}")
    public ResponseEntity<Void> deleteConversation(
            @Parameter(description = "Conversation ID") @PathVariable Long id) {
        conversationService.deleteConversation(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Create conversation with tutor", description = "Creates a conversation between a student and tutor using student's userId and tutor's tutorId")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Conversation successfully created or found"),
        @ApiResponse(responseCode = "400", description = "Invalid input or users not found")
    })
    @PostMapping("/createWithTutor/{studentUserId}/{tutorId}")
    public ResponseEntity<ConversationDTO> createConversationWithTutor(
            @Parameter(description = "Student user ID") @PathVariable Long studentUserId,
            @Parameter(description = "Tutor profile ID") @PathVariable Long tutorId) {
        try {
            // Get student user
            UserEntity studentUser = userService.getUserById(studentUserId)
                    .orElseThrow(() -> new RuntimeException("Student user not found with ID: " + studentUserId));

            // Get tutor's userId from tutorId
            Long tutorUserId = tutorProfileService.getUserIdFromTutorId(tutorId);

            // Debug logging to verify IDs
            System.out.println("DEBUG: Creating conversation between student userId: " + studentUserId +
                              " and tutor userId: " + tutorUserId + " (from tutorId: " + tutorId + ")");

            // Safety check - ensure the userIds are different
            if (studentUserId.equals(tutorUserId)) {
                System.out.println("ERROR: Cannot create conversation between the same user! " +
                                 "studentUserId: " + studentUserId + ", tutorUserId: " + tutorUserId);
                return ResponseEntity.badRequest().body(null);
            }

            // Get tutor user
            UserEntity tutorUser = userService.getUserById(tutorUserId)
                    .orElseThrow(() -> new RuntimeException("Tutor user not found with ID: " + tutorUserId));

            // Find or create the conversation
            ConversationEntity conversation = conversationService.findOrCreateStudentTutorConversation(studentUser, tutorUser);

            // Verify created conversation
            System.out.println("DEBUG: Created conversation ID: " + conversation.getConversationId() + 
                              " between student: " + conversation.getStudent().getUserId() + 
                              " and tutor: " + conversation.getTutor().getUserId());

            return ResponseEntity.ok(conversationDTOMapper.toDTO(conversation));
        } catch (RuntimeException e) {
            // Log the full error for debugging
            System.out.println("ERROR in createConversationWithTutor: " + e.getMessage());
            e.printStackTrace();

            // Return 404 for "not found" errors
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(null);
            }

            // Return 400 for other runtime exceptions
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            // Log the full error for debugging
            System.out.println("ERROR in createConversationWithTutor: " + e.getMessage());
            e.printStackTrace();

            // Return 500 for unexpected errors
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }

    @Operation(summary = "Update conversation names", description = "Updates the user names for an existing conversation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Conversation names successfully updated"),
        @ApiResponse(responseCode = "404", description = "Conversation not found")
    })
    @PutMapping("/updateNames/{conversationId}")
    public ResponseEntity<ConversationDTO> updateConversationNames(
            @Parameter(description = "Conversation ID") @PathVariable Long conversationId) {
        try {
            // Get the conversation
            Optional<ConversationEntity> conversationOpt = conversationService.getConversationById(conversationId);
            if (!conversationOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            ConversationEntity conversation = conversationOpt.get();
            
            // No need to modify the conversation - the DTO mapper will set the names correctly
            
            // Return the conversation with updated names
            return ResponseEntity.ok(conversationDTOMapper.toDTO(conversation));
        } catch (Exception e) {
            System.out.println("Error updating conversation names: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
} 
