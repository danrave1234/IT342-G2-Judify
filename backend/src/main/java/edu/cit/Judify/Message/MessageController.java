package edu.cit.Judify.Message;

import edu.cit.Judify.Conversation.ConversationEntity;
import edu.cit.Judify.Message.DTO.MessageDTO;
import edu.cit.Judify.Message.DTO.MessageDTOMapper;
import edu.cit.Judify.User.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
public class MessageController {

    private final MessageService messageService;
    private final MessageDTOMapper messageDTOMapper;

    @Autowired
    public MessageController(MessageService messageService, MessageDTOMapper messageDTOMapper) {
        this.messageService = messageService;
        this.messageDTOMapper = messageDTOMapper;
    }

    @PostMapping("/sendMessage")
    public ResponseEntity<MessageDTO> createMessage(@RequestBody MessageDTO messageDTO) {
        MessageEntity message = messageDTOMapper.toEntity(messageDTO, null, null); // These will be set by the service
        return ResponseEntity.ok(messageDTOMapper.toDTO(messageService.createMessage(message)));
    }

    @GetMapping("/findById/{id}")
    public ResponseEntity<MessageDTO> getMessageById(@PathVariable Long id) {
        return messageService.getMessageById(id)
                .map(messageDTOMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/findByConversation/{conversationId}")
    public ResponseEntity<List<MessageDTO>> getConversationMessages(
            @PathVariable ConversationEntity conversation) {
        return ResponseEntity.ok(messageService.getConversationMessages(conversation)
                .stream()
                .map(messageDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/findUnreadBySender/{senderId}")
    public ResponseEntity<List<MessageDTO>> getUnreadMessagesBySender(
            @PathVariable UserEntity sender) {
        return ResponseEntity.ok(messageService.getUnreadMessagesBySender(sender)
                .stream()
                .map(messageDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/findUnreadByConversation/{conversationId}")
    public ResponseEntity<List<MessageDTO>> getUnreadConversationMessages(
            @PathVariable ConversationEntity conversation) {
        return ResponseEntity.ok(messageService.getUnreadConversationMessages(conversation)
                .stream()
                .map(messageDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/findByConversationPaginated/{conversationId}")
    public ResponseEntity<Page<MessageDTO>> getConversationMessagesPaginated(
            @PathVariable ConversationEntity conversation,
            @RequestParam(defaultValue = "DESC") String order,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<MessageEntity> messages = messageService.getConversationMessagesPaginated(
                conversation, order, page, size);
                
        Page<MessageDTO> messageDTOs = messages.map(messageDTOMapper::toDTO);
        return ResponseEntity.ok(messageDTOs);
    }
    
    @GetMapping("/findUnreadBySenderPaginated/{senderId}")
    public ResponseEntity<Page<MessageDTO>> getUnreadMessagesBySenderPaginated(
            @PathVariable UserEntity sender,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<MessageEntity> messages = messageService.getUnreadMessagesBySenderPaginated(
                sender, page, size);
                
        Page<MessageDTO> messageDTOs = messages.map(messageDTOMapper::toDTO);
        return ResponseEntity.ok(messageDTOs);
    }
    
    @GetMapping("/findUnreadByConversationPaginated/{conversationId}")
    public ResponseEntity<Page<MessageDTO>> getUnreadConversationMessagesPaginated(
            @PathVariable ConversationEntity conversation,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<MessageEntity> messages = messageService.getUnreadConversationMessagesPaginated(
                conversation, page, size);
                
        Page<MessageDTO> messageDTOs = messages.map(messageDTOMapper::toDTO);
        return ResponseEntity.ok(messageDTOs);
    }

    @PutMapping("/markAsRead/{id}")
    public ResponseEntity<MessageDTO> markMessageAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(messageDTOMapper.toDTO(messageService.markMessageAsRead(id)));
    }

    @PutMapping("/markAllAsRead/{conversationId}")
    public ResponseEntity<Void> markAllConversationMessagesAsRead(
            @PathVariable ConversationEntity conversation) {
        messageService.markAllConversationMessagesAsRead(conversation);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/deleteMessage/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        messageService.deleteMessage(id);
        return ResponseEntity.ok().build();
    }
} 