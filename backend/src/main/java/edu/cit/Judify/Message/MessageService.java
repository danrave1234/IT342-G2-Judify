package edu.cit.Judify.Message;

import edu.cit.Judify.Conversation.ConversationEntity;
import edu.cit.Judify.User.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MessageService {

    private final MessageRepository messageRepository;

    @Autowired
    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Transactional
    public MessageEntity createMessage(MessageEntity message) {
        return messageRepository.save(message);
    }

    public Optional<MessageEntity> getMessageById(Long id) {
        return messageRepository.findById(id);
    }

    public List<MessageEntity> getConversationMessages(ConversationEntity conversation) {
        return messageRepository.findByConversationOrderByTimestampDesc(conversation);
    }

    public List<MessageEntity> getUnreadMessagesBySender(UserEntity sender) {
        return messageRepository.findBySenderAndIsReadFalse(sender);
    }

    public List<MessageEntity> getUnreadConversationMessages(ConversationEntity conversation) {
        return messageRepository.findByConversationAndIsReadFalse(conversation);
    }

    @Transactional
    public MessageEntity markMessageAsRead(Long id) {
        MessageEntity message = messageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        
        message.setIsRead(true);
        return messageRepository.save(message);
    }

    @Transactional
    public void markAllConversationMessagesAsRead(ConversationEntity conversation) {
        List<MessageEntity> unreadMessages = messageRepository.findByConversationAndIsReadFalse(conversation);
        unreadMessages.forEach(message -> message.setIsRead(true));
        messageRepository.saveAll(unreadMessages);
    }

    @Transactional
    public void deleteMessage(Long id) {
        messageRepository.deleteById(id);
    }

    // Paginated version of getConversationMessages
    public Page<MessageEntity> getConversationMessagesPaginated(
            ConversationEntity conversation, String order, int page, int size) {
        
        Sort sort = order.equalsIgnoreCase("ASC") ?
                Sort.by("timestamp").ascending() :
                Sort.by("timestamp").descending();
                
        Pageable pageable = PageRequest.of(page, size, sort);
        return messageRepository.findByConversation(conversation, pageable);
    }
    
    // Paginated version of getUnreadMessagesBySender
    public Page<MessageEntity> getUnreadMessagesBySenderPaginated(
            UserEntity sender, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return messageRepository.findBySenderAndIsReadFalse(sender, pageable);
    }
    
    // Paginated version of getUnreadConversationMessages
    public Page<MessageEntity> getUnreadConversationMessagesPaginated(
            ConversationEntity conversation, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return messageRepository.findByConversationAndIsReadFalse(conversation, pageable);
    }
} 