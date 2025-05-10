package edu.cit.Judify.Message;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import edu.cit.Judify.Message.DTO.MessageDTO;
import edu.cit.Judify.Message.DTO.MessageDTOMapper;
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
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
@Tag(name = "Messages", description = "Message management endpoints")
public class MessageController {

    private final MessageService messageService;
    private final ConversationService conversationService;
    private final UserService userService;
    private final MessageDTOMapper messageDTOMapper;

    @Autowired
    public MessageController(MessageService messageService, ConversationService conversationService,
            UserService userService, MessageDTOMapper messageDTOMapper) {
        this.messageService = messageService;
        this.conversationService = conversationService;
        this.userService = userService;
        this.messageDTOMapper = messageDTOMapper;
    }

    @Operation(summary = "Send a message", description = "Sends a new message in a conversation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message sent successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageDTO.class))),
        @ApiResponse(responseCode = "404", description = "User or conversation not found"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping("/sendMessage")
    public ResponseEntity<MessageDTO> sendMessage(@RequestBody MessageDTO messageDTO) {
        try {
            // Send the message using the new service method
            MessageEntity message = messageService.sendMessage(messageDTO);
            return ResponseEntity.ok(messageDTOMapper.toDTO(message));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @Operation(summary = "Get messages by conversation", description = "Gets all messages in a conversation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved messages"),
        @ApiResponse(responseCode = "404", description = "Conversation not found")
    })
    @GetMapping("/findByConversation/{conversationId}")
    public ResponseEntity<List<MessageDTO>> getMessagesByConversation(
            @Parameter(description = "Conversation ID") @PathVariable Long conversationId) {
        try {
            // Check if conversation exists
            conversationService.getConversationById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found with ID: " + conversationId));
            
            // Get messages
            List<MessageEntity> messages = messageService.getMessagesByConversationId(conversationId);
            
            // Convert to DTOs
            List<MessageDTO> messageDTOs = messages.stream()
                    .map(messageDTOMapper::toDTO)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(messageDTOs);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get messages by conversation with pagination", description = "Gets messages in a conversation with pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved messages"),
        @ApiResponse(responseCode = "404", description = "Conversation not found")
    })
    @GetMapping("/findByConversationPaginated/{conversationId}")
    public ResponseEntity<Page<MessageDTO>> getMessagesByConversationPaginated(
            @Parameter(description = "Conversation ID") @PathVariable Long conversationId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        try {
            // Check if conversation exists
            conversationService.getConversationById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found with ID: " + conversationId));
            
            // Create pageable request
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
            
            // Get messages
            Page<MessageEntity> messagePage = messageService.getMessagesByConversationIdPaginated(conversationId, pageRequest);
            
            // Convert to DTOs
            Page<MessageDTO> messageDTOPage = messagePage.map(messageDTOMapper::toDTO);
            
            return ResponseEntity.ok(messageDTOPage);
        } catch (Exception e) {
            e.printStackTrace(); // Log the exception
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Mark message as read", description = "Marks a message as read")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message marked as read"),
        @ApiResponse(responseCode = "404", description = "Message not found")
    })
    @PutMapping("/markAsRead/{messageId}")
    public ResponseEntity<MessageDTO> markMessageAsRead(
            @Parameter(description = "Message ID") @PathVariable Long messageId) {
        try {
            MessageEntity message = messageService.markMessageAsRead(messageId);
            return ResponseEntity.ok(messageDTOMapper.toDTO(message));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Mark all messages as read", description = "Marks all messages in a conversation as read for a user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Messages marked as read",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Integer.class))),
        @ApiResponse(responseCode = "404", description = "Conversation not found")
    })
    @PutMapping("/markAllAsRead/{conversationId}")
    public ResponseEntity<Integer> markAllMessagesAsRead(
            @Parameter(description = "Conversation ID") @PathVariable Long conversationId,
            @Parameter(description = "User ID") @RequestParam Long userId) {
        try {
            // Check if conversation exists
            conversationService.getConversationById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found with ID: " + conversationId));
            
            // Mark all messages as read for the user
            int updatedCount = messageService.markAllMessagesAsRead(conversationId, userId);
            
            return ResponseEntity.ok(updatedCount);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get count of unread messages", description = "Gets the count of unread messages in a conversation for a user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved unread count"),
        @ApiResponse(responseCode = "404", description = "Conversation not found")
    })
    @GetMapping("/unreadCount/{conversationId}")
    public ResponseEntity<Long> getUnreadMessageCount(
            @Parameter(description = "Conversation ID") @PathVariable Long conversationId,
            @Parameter(description = "User ID") @RequestParam Long userId) {
        try {
            // Check if conversation exists
            conversationService.getConversationById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found with ID: " + conversationId));
            
            // Get unread count
            Long unreadCount = messageService.getUnreadMessagesCount(conversationId, userId);
            
            return ResponseEntity.ok(unreadCount);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get latest messages for user", description = "Gets the latest message in each conversation for a user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved latest messages"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/latestForUser/{userId}")
    public ResponseEntity<List<MessageDTO>> getLatestMessagesForUser(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        try {
            // Check if user exists
            userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
            
            // Get latest messages
            List<MessageEntity> messages = messageService.getLatestMessagesForUser(userId);
            
            // Convert to DTOs
            List<MessageDTO> messageDTOs = messages.stream()
                    .map(messageDTOMapper::toDTO)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(messageDTOs);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get message by ID", description = "Returns a message by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the message"),
        @ApiResponse(responseCode = "404", description = "Message not found")
    })
    @GetMapping("/findById/{id}")
    public ResponseEntity<MessageDTO> getMessageById(
            @Parameter(description = "Message ID") @PathVariable Long id) {
        return messageService.getMessageById(id)
                .map(messageDTOMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get unread messages by sender", description = "Returns all unread messages from a specific sender")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved unread messages by sender")
    })
    @GetMapping("/findUnreadBySender/{senderId}")
    public ResponseEntity<List<MessageDTO>> getUnreadMessagesBySender(
            @Parameter(description = "Sender ID") @PathVariable Long senderId) {
        UserEntity sender = userService.getUserById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + senderId));
        return ResponseEntity.ok(messageService.getUnreadMessagesBySender(sender)
                .stream()
                .map(messageDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get unread messages by conversation", description = "Returns all unread messages in a specific conversation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved unread conversation messages")
    })
    @GetMapping("/findUnreadByConversation/{conversationId}")
    public ResponseEntity<List<MessageDTO>> getUnreadConversationMessages(
            @Parameter(description = "Conversation ID") @PathVariable Long conversationId) {
        ConversationEntity conversation = conversationService.getConversationById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found with ID: " + conversationId));
        return ResponseEntity.ok(messageService.getUnreadConversationMessages(conversation)
                .stream()
                .map(messageDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get paginated unread messages by sender", description = "Returns a paginated list of unread messages from a specific sender")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated unread messages by sender")
    })
    @GetMapping("/findUnreadBySenderPaginated/{senderId}")
    public ResponseEntity<Page<MessageDTO>> getUnreadMessagesBySenderPaginated(
            @Parameter(description = "Sender ID") @PathVariable Long senderId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        UserEntity sender = userService.getUserById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + senderId));
        Page<MessageEntity> messages = messageService.getUnreadMessagesBySenderPaginated(
                sender, page, size);

        Page<MessageDTO> messageDTOs = messages.map(messageDTOMapper::toDTO);
        return ResponseEntity.ok(messageDTOs);
    }

    @Operation(summary = "Get paginated unread messages by conversation", description = "Returns a paginated list of unread messages in a specific conversation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated unread conversation messages")
    })
    @GetMapping("/findUnreadByConversationPaginated/{conversationId}")
    public ResponseEntity<Page<MessageDTO>> getUnreadConversationMessagesPaginated(
            @Parameter(description = "Conversation ID") @PathVariable Long conversationId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        ConversationEntity conversation = conversationService.getConversationById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found with ID: " + conversationId));
        Page<MessageEntity> messages = messageService.getUnreadConversationMessagesPaginated(
                conversation, page, size);

        Page<MessageDTO> messageDTOs = messages.map(messageDTOMapper::toDTO);
        return ResponseEntity.ok(messageDTOs);
    }

    @Operation(summary = "Delete a message", description = "Deletes a message by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message successfully deleted"),
        @ApiResponse(responseCode = "404", description = "Message not found")
    })
    @DeleteMapping("/deleteMessage/{id}")
    public ResponseEntity<Void> deleteMessage(
            @Parameter(description = "Message ID") @PathVariable Long id) {
        messageService.deleteMessage(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get messages between users", description = "Returns all messages between two specific users")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved messages between users")
    })
    @GetMapping("/findBetweenUsers/{user1Id}/{user2Id}")
    public ResponseEntity<List<MessageDTO>> getMessagesBetweenUsers(
            @Parameter(description = "First user ID") @PathVariable Long user1Id,
            @Parameter(description = "Second user ID") @PathVariable Long user2Id) {
        
        // Get user entities
        UserEntity user1 = userService.getUserById(user1Id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + user1Id));
        UserEntity user2 = userService.getUserById(user2Id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + user2Id));
        
        // Get messages between users using the service
        List<MessageEntity> messages = messageService.getMessagesBetweenUsers(user1, user2);
        
        return ResponseEntity.ok(messages
                .stream()
                .map(messageDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }
} 
