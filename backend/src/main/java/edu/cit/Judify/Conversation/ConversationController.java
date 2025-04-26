package edu.cit.Judify.Conversation;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
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
import edu.cit.Judify.User.UserEntity;
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

    @Autowired
    public ConversationController(ConversationService conversationService, ConversationDTOMapper conversationDTOMapper) {
        this.conversationService = conversationService;
        this.conversationDTOMapper = conversationDTOMapper;
    }

    @Operation(summary = "Create a new conversation", description = "Creates a new conversation between users")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Conversation successfully created",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConversationDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping("/createConversation")
    public ResponseEntity<ConversationDTO> createConversation(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Conversation data to create", required = true)
            @RequestBody ConversationDTO conversationDTO) {
        ConversationEntity conversation = conversationDTOMapper.toEntity(conversationDTO, null); // Participants will be set by the service
        return ResponseEntity.ok(conversationDTOMapper.toDTO(conversationService.createConversation(conversation)));
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
        UserEntity participant = new UserEntity();
        participant.setUserId(userId);
        return ResponseEntity.ok(conversationService.getUserConversations(participant)
                .stream()
                .map(conversationDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Add participant to conversation", description = "Adds a user to an existing conversation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Participant successfully added to conversation"),
        @ApiResponse(responseCode = "404", description = "Conversation not found")
    })
    @PutMapping("/addParticipant/{id}")
    public ResponseEntity<ConversationDTO> addParticipant(
            @Parameter(description = "Conversation ID") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "User to add to conversation", required = true)
            @RequestBody UserEntity participant) {
        return ResponseEntity.ok(conversationDTOMapper.toDTO(conversationService.addParticipant(id, participant)));
    }

    @Operation(summary = "Remove participant from conversation", description = "Removes a user from an existing conversation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Participant successfully removed from conversation"),
        @ApiResponse(responseCode = "404", description = "Conversation not found")
    })
    @PutMapping("/removeParticipant/{id}")
    public ResponseEntity<ConversationDTO> removeParticipant(
            @Parameter(description = "Conversation ID") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "User to remove from conversation", required = true)
            @RequestBody UserEntity participant) {
        return ResponseEntity.ok(conversationDTOMapper.toDTO(conversationService.removeParticipant(id, participant)));
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
} 