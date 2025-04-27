package edu.cit.Judify.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cit.Judify.Conversation.ConversationEntity;
import edu.cit.Judify.Conversation.ConversationRepository;
import edu.cit.Judify.Message.DTO.MessageDTO;
import edu.cit.Judify.User.UserEntity;
import edu.cit.Judify.User.UserRepository;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    @Autowired
    public MessageService(MessageRepository messageRepository, 
                          ConversationRepository conversationRepository,
                          UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Send a message in a conversation
     * @param messageDTO The message data
     * @return The created message
     * @throws RuntimeException if the conversation, sender, or receiver is not found
     */
    @Transactional
    public MessageEntity sendMessage(MessageDTO messageDTO) {
        // Validate conversation
        ConversationEntity conversation = conversationRepository.findById(messageDTO.getConversationId())
                .orElseThrow(() -> new RuntimeException("Conversation not found with ID: " + messageDTO.getConversationId()));
        
        // Validate sender
        UserEntity sender = userRepository.findById(messageDTO.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender not found with ID: " + messageDTO.getSenderId()));
        
        // Validate receiver
        UserEntity receiver = userRepository.findById(messageDTO.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found with ID: " + messageDTO.getReceiverId()));
        
        // Verify that sender and receiver are part of the conversation
        if (!(conversation.getUser1().getUserId().equals(sender.getUserId()) || conversation.getUser2().getUserId().equals(sender.getUserId()))) {
            throw new RuntimeException("Sender is not part of the conversation");
        }
        
        if (!(conversation.getUser1().getUserId().equals(receiver.getUserId()) || conversation.getUser2().getUserId().equals(receiver.getUserId()))) {
            throw new RuntimeException("Receiver is not part of the conversation");
        }
        
        // Create and save the message
        MessageEntity message = new MessageEntity();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(messageDTO.getContent());
        message.setIsRead(false);
        
        return messageRepository.save(message);
    }

    /**
     * Get all messages in a conversation
     * @param conversationId The ID of the conversation
     * @return List of messages in the conversation
     */
    @Transactional(readOnly = true)
    public List<MessageEntity> getMessagesByConversationId(Long conversationId) {
        return messageRepository.findByConversationConversationIdOrderByTimestampAsc(conversationId);
    }
    
    /**
     * Get all messages in a conversation with pagination
     * @param conversationId The ID of the conversation
     * @param pageable Pagination information
     * @return Page of messages in the conversation
     */
    @Transactional(readOnly = true)
    public Page<MessageEntity> getMessagesByConversationIdPaginated(Long conversationId, Pageable pageable) {
        return messageRepository.findByConversationConversationIdOrderByTimestampDesc(conversationId, pageable);
    }

    /**
     * Get a specific message by ID
     * @param messageId The ID of the message
     * @return Optional containing the message if found
     */
    @Transactional(readOnly = true)
    public Optional<MessageEntity> getMessageById(Long messageId) {
        return messageRepository.findById(messageId);
    }

    /**
     * Mark a message as read
     * @param messageId The ID of the message to mark as read
     * @return The updated message
     * @throws RuntimeException if the message is not found
     */
    @Transactional
    public MessageEntity markMessageAsRead(Long messageId) {
        MessageEntity message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found with ID: " + messageId));
        message.setIsRead(true);
        return messageRepository.save(message);
    }
    
    /**
     * Mark all messages in a conversation as read for a specific user (the receiver)
     * @param conversationId The ID of the conversation
     * @param userId The ID of the user whose messages to mark as read
     * @return The number of messages updated
     */
    @Transactional
    public int markAllMessagesAsRead(Long conversationId, Long userId) {
        // Get the user entity
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
                
        // Mark all unread messages where the user is the receiver
        return messageRepository.markAllAsReadInConversationForUser(conversationId, user);
    }

    /**
     * Get all messages in a conversation ordered by timestamp (newest first)
     * @param conversation The conversation
     * @return List of messages
     */
    public List<MessageEntity> getConversationMessages(ConversationEntity conversation) {
        return messageRepository.findByConversationOrderByTimestampDesc(conversation);
    }

    /**
     * Get unread messages sent by a specific user
     * @param sender The sender
     * @return List of unread messages
     */
    public List<MessageEntity> getUnreadMessagesBySender(UserEntity sender) {
        return messageRepository.findBySenderAndIsReadFalse(sender);
    }

    /**
     * Get unread messages in a conversation
     * @param conversation The conversation
     * @return List of unread messages
     */
    public List<MessageEntity> getUnreadConversationMessages(ConversationEntity conversation) {
        return messageRepository.findByConversationAndIsReadFalse(conversation);
    }
    
    /**
     * Get all messages between two users (in both directions)
     * @param user1 First user
     * @param user2 Second user
     * @return List of messages between the two users, sorted by timestamp (newest first)
     */
    public List<MessageEntity> getMessagesBetweenUsers(UserEntity user1, UserEntity user2) {
        List<MessageEntity> user1ToUser2Messages = messageRepository.findBySenderAndReceiver(user1, user2);
        List<MessageEntity> user2ToUser1Messages = messageRepository.findBySenderAndReceiver(user2, user1);
        
        List<MessageEntity> allMessages = new ArrayList<>(user1ToUser2Messages);
        allMessages.addAll(user2ToUser1Messages);
        
        // Sort by timestamp (newest first)
        allMessages.sort((m1, m2) -> m2.getTimestamp().compareTo(m1.getTimestamp()));
        
        return allMessages;
    }

    /**
     * Mark all unread messages in a conversation as read
     * @param conversation The conversation
     */
    @Transactional
    public void markAllConversationMessagesAsRead(ConversationEntity conversation) {
        List<MessageEntity> unreadMessages = messageRepository.findByConversationAndIsReadFalse(conversation);
        unreadMessages.forEach(message -> message.setIsRead(true));
        messageRepository.saveAll(unreadMessages);
    }

    /**
     * Delete a message by ID
     * @param id The message ID
     */
    @Transactional
    public void deleteMessage(Long id) {
        messageRepository.deleteById(id);
    }

    /**
     * Get messages in a conversation with pagination
     * @param conversation The conversation
     * @param order The sort order (ASC or DESC)
     * @param page The page number (0-based)
     * @param size The page size
     * @return Page of messages
     */
    public Page<MessageEntity> getConversationMessagesPaginated(
            ConversationEntity conversation, String order, int page, int size) {
        
        Sort sort = order.equalsIgnoreCase("ASC") ?
                Sort.by("timestamp").ascending() :
                Sort.by("timestamp").descending();
                
        Pageable pageable = PageRequest.of(page, size, sort);
        return messageRepository.findByConversation(conversation, pageable);
    }
    
    /**
     * Get unread messages from a sender with pagination
     * @param sender The sender
     * @param page The page number (0-based)
     * @param size The page size
     * @return Page of unread messages
     */
    public Page<MessageEntity> getUnreadMessagesBySenderPaginated(
            UserEntity sender, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return messageRepository.findBySenderAndIsReadFalse(sender, pageable);
    }
    
    /**
     * Get unread messages in a conversation with pagination
     * @param conversation The conversation
     * @param page The page number (0-based)
     * @param size The page size
     * @return Page of unread messages
     */
    public Page<MessageEntity> getUnreadConversationMessagesPaginated(
            ConversationEntity conversation, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return messageRepository.findByConversationAndIsReadFalse(conversation, pageable);
    }

    /**
     * Get count of unread messages in a conversation for a user
     * @param conversationId The ID of the conversation
     * @param userId The ID of the user
     * @return Count of unread messages
     */
    @Transactional(readOnly = true)
    public Long getUnreadMessagesCount(Long conversationId, Long userId) {
        return messageRepository.countUnreadMessagesInConversationForUser(conversationId, userId);
    }
    
    /**
     * Get latest messages for a user across all conversations
     * @param userId The ID of the user
     * @return List of latest messages
     */
    @Transactional(readOnly = true)
    public List<MessageEntity> getLatestMessagesForUser(Long userId) {
        return messageRepository.findLatestMessagesForUser(userId);
    }
    
    /**
     * Get all messages for a user where they are sender or receiver
     * @param user The user
     * @return List of messages
     */
    @Transactional(readOnly = true)
    public List<MessageEntity> getAllUserMessages(UserEntity user) {
        List<MessageEntity> sentMessages = messageRepository.findBySender(user);
        List<MessageEntity> receivedMessages = messageRepository.findByReceiver(user);
        
        // Combine the lists
        List<MessageEntity> allMessages = sentMessages;
        allMessages.addAll(receivedMessages);
        
        return allMessages;
    }
} 