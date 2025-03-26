package edu.cit.Judify.Message;

import edu.cit.Judify.Conversation.ConversationEntity;
import edu.cit.Judify.Message.DTO.MessageDTO;
import edu.cit.Judify.Message.DTO.MessageDTOMapper;
import edu.cit.Judify.User.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
@Tag(name = "Messages", description = "Message management endpoints")
public class MessageController {

    private final MessageService messageService;
    private final MessageDTOMapper messageDTOMapper;

    @Autowired
    public MessageController(MessageService messageService, MessageDTOMapper messageDTOMapper) {
        this.messageService = messageService;
        this.messageDTOMapper = messageDTOMapper;
    }

    @Operation(summary = "Send a new message", description = "Creates and sends a new message in a conversation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message successfully sent",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping("/sendMessage")
    public ResponseEntity<MessageDTO> createMessage(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Message data to send", required = true)
            @RequestBody MessageDTO messageDTO) {
        MessageEntity message = messageDTOMapper.toEntity(messageDTO, null, null); // These will be set by the service
        return ResponseEntity.ok(messageDTOMapper.toDTO(messageService.createMessage(message)));
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

    @Operation(summary = "Get messages by conversation", description = "Returns all messages in a specific conversation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved conversation messages")
    })
    @GetMapping("/findByConversation/{conversationId}")
    public ResponseEntity<List<MessageDTO>> getConversationMessages(
            @Parameter(description = "Conversation ID") @PathVariable ConversationEntity conversation) {
        return ResponseEntity.ok(messageService.getConversationMessages(conversation)
                .stream()
                .map(messageDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get unread messages by sender", description = "Returns all unread messages from a specific sender")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved unread messages by sender")
    })
    @GetMapping("/findUnreadBySender/{senderId}")
    public ResponseEntity<List<MessageDTO>> getUnreadMessagesBySender(
            @Parameter(description = "Sender ID") @PathVariable UserEntity sender) {
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
            @Parameter(description = "Conversation ID") @PathVariable ConversationEntity conversation) {
        return ResponseEntity.ok(messageService.getUnreadConversationMessages(conversation)
                .stream()
                .map(messageDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get paginated messages by conversation", description = "Returns a paginated list of messages in a specific conversation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated conversation messages")
    })
    @GetMapping("/findByConversationPaginated/{conversationId}")
    public ResponseEntity<Page<MessageDTO>> getConversationMessagesPaginated(
            @Parameter(description = "Conversation ID") @PathVariable ConversationEntity conversation,
            @Parameter(description = "Sort order (ASC or DESC)") @RequestParam(defaultValue = "DESC") String order,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        
        Page<MessageEntity> messages = messageService.getConversationMessagesPaginated(
                conversation, order, page, size);
                
        Page<MessageDTO> messageDTOs = messages.map(messageDTOMapper::toDTO);
        return ResponseEntity.ok(messageDTOs);
    }
    
    @Operation(summary = "Get paginated unread messages by sender", description = "Returns a paginated list of unread messages from a specific sender")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated unread messages by sender")
    })
    @GetMapping("/findUnreadBySenderPaginated/{senderId}")
    public ResponseEntity<Page<MessageDTO>> getUnreadMessagesBySenderPaginated(
            @Parameter(description = "Sender ID") @PathVariable UserEntity sender,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        
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
            @Parameter(description = "Conversation ID") @PathVariable ConversationEntity conversation,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        
        Page<MessageEntity> messages = messageService.getUnreadConversationMessagesPaginated(
                conversation, page, size);
                
        Page<MessageDTO> messageDTOs = messages.map(messageDTOMapper::toDTO);
        return ResponseEntity.ok(messageDTOs);
    }

    @Operation(summary = "Mark message as read", description = "Marks a specific message as read")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message successfully marked as read"),
        @ApiResponse(responseCode = "404", description = "Message not found")
    })
    @PutMapping("/markAsRead/{id}")
    public ResponseEntity<MessageDTO> markMessageAsRead(
            @Parameter(description = "Message ID") @PathVariable Long id) {
        return ResponseEntity.ok(messageDTOMapper.toDTO(messageService.markMessageAsRead(id)));
    }

    @Operation(summary = "Mark all messages in conversation as read", description = "Marks all messages in a specific conversation as read")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All messages in conversation successfully marked as read")
    })
    @PutMapping("/markAllAsRead/{conversationId}")
    public ResponseEntity<Void> markAllConversationMessagesAsRead(
            @Parameter(description = "Conversation ID") @PathVariable ConversationEntity conversation) {
        messageService.markAllConversationMessagesAsRead(conversation);
        return ResponseEntity.ok().build();
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
} 