package edu.cit.Judify.Message;

import edu.cit.Judify.Conversation.ConversationEntity;
import edu.cit.Judify.User.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
public class MessageController {

    private final MessageService messageService;

    @Autowired
    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public ResponseEntity<MessageEntity> createMessage(@RequestBody MessageEntity message) {
        return ResponseEntity.ok(messageService.createMessage(message));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MessageEntity> getMessageById(@PathVariable Long id) {
        return messageService.getMessageById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<MessageEntity>> getConversationMessages(
            @PathVariable ConversationEntity conversation) {
        return ResponseEntity.ok(messageService.getConversationMessages(conversation));
    }

    @GetMapping("/unread/sender/{senderId}")
    public ResponseEntity<List<MessageEntity>> getUnreadMessagesBySender(
            @PathVariable UserEntity sender) {
        return ResponseEntity.ok(messageService.getUnreadMessagesBySender(sender));
    }

    @GetMapping("/unread/conversation/{conversationId}")
    public ResponseEntity<List<MessageEntity>> getUnreadConversationMessages(
            @PathVariable ConversationEntity conversation) {
        return ResponseEntity.ok(messageService.getUnreadConversationMessages(conversation));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<MessageEntity> markMessageAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(messageService.markMessageAsRead(id));
    }

    @PutMapping("/conversation/{conversationId}/read-all")
    public ResponseEntity<Void> markAllConversationMessagesAsRead(
            @PathVariable ConversationEntity conversation) {
        messageService.markAllConversationMessagesAsRead(conversation);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        messageService.deleteMessage(id);
        return ResponseEntity.ok().build();
    }
} 