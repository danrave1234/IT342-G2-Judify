package edu.cit.Judify.Conversation;

import edu.cit.Judify.Conversation.DTO.ConversationDTO;
import edu.cit.Judify.Conversation.DTO.ConversationDTOMapper;
import edu.cit.Judify.User.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/conversations")
@CrossOrigin(origins = "*")
public class ConversationController {

    private final ConversationService conversationService;
    private final ConversationDTOMapper conversationDTOMapper;

    @Autowired
    public ConversationController(ConversationService conversationService, ConversationDTOMapper conversationDTOMapper) {
        this.conversationService = conversationService;
        this.conversationDTOMapper = conversationDTOMapper;
    }

    @PostMapping("/createConversation")
    public ResponseEntity<ConversationDTO> createConversation(@RequestBody ConversationDTO conversationDTO) {
        ConversationEntity conversation = conversationDTOMapper.toEntity(conversationDTO, null); // Participants will be set by the service
        return ResponseEntity.ok(conversationDTOMapper.toDTO(conversationService.createConversation(conversation)));
    }

    @GetMapping("/findById/{id}")
    public ResponseEntity<ConversationDTO> getConversationById(@PathVariable Long id) {
        return conversationService.getConversationById(id)
                .map(conversationDTOMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/findByUser/{userId}")
    public ResponseEntity<List<ConversationDTO>> getUserConversations(@PathVariable UserEntity participant) {
        return ResponseEntity.ok(conversationService.getUserConversations(participant)
                .stream()
                .map(conversationDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @PutMapping("/addParticipant/{id}")
    public ResponseEntity<ConversationDTO> addParticipant(
            @PathVariable Long id,
            @RequestBody UserEntity participant) {
        return ResponseEntity.ok(conversationDTOMapper.toDTO(conversationService.addParticipant(id, participant)));
    }

    @PutMapping("/removeParticipant/{id}")
    public ResponseEntity<ConversationDTO> removeParticipant(
            @PathVariable Long id,
            @RequestBody UserEntity participant) {
        return ResponseEntity.ok(conversationDTOMapper.toDTO(conversationService.removeParticipant(id, participant)));
    }

    @DeleteMapping("/deleteConversation/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long id) {
        conversationService.deleteConversation(id);
        return ResponseEntity.ok().build();
    }
} 