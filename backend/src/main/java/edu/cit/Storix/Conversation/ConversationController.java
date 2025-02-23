package edu.cit.Storix.Conversation;

import edu.cit.Storix.User.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@CrossOrigin(origins = "*")
public class ConversationController {

    private final ConversationService conversationService;

    @Autowired
    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public ResponseEntity<ConversationEntity> createConversation(@RequestBody ConversationEntity conversation) {
        return ResponseEntity.ok(conversationService.createConversation(conversation));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationEntity> getConversationById(@PathVariable Long id) {
        return conversationService.getConversationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ConversationEntity>> getUserConversations(@PathVariable UserEntity participant) {
        return ResponseEntity.ok(conversationService.getUserConversations(participant));
    }

    @PutMapping("/{id}/participants/add")
    public ResponseEntity<ConversationEntity> addParticipant(
            @PathVariable Long id,
            @RequestBody UserEntity participant) {
        return ResponseEntity.ok(conversationService.addParticipant(id, participant));
    }

    @PutMapping("/{id}/participants/remove")
    public ResponseEntity<ConversationEntity> removeParticipant(
            @PathVariable Long id,
            @RequestBody UserEntity participant) {
        return ResponseEntity.ok(conversationService.removeParticipant(id, participant));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long id) {
        conversationService.deleteConversation(id);
        return ResponseEntity.ok().build();
    }
} 