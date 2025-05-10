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
import edu.cit.Judify.TutoringSession.TutoringSessionEntity;
import edu.cit.Judify.TutoringSession.TutoringSessionRepository;
import edu.cit.Judify.User.UserEntity;
import edu.cit.Judify.User.UserRepository;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final TutoringSessionRepository tutoringSessionRepository;

    @Autowired
    public MessageService(MessageRepository messageRepository, 
                          ConversationRepository conversationRepository,
                          UserRepository userRepository,
                          TutoringSessionRepository tutoringSessionRepository) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.tutoringSessionRepository = tutoringSessionRepository;
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
        if (!(conversation.getStudent().getUserId().equals(sender.getUserId()) || conversation.getTutor().getUserId().equals(sender.getUserId()))) {
            throw new RuntimeException("Sender is not part of the conversation");
        }

        if (!(conversation.getStudent().getUserId().equals(receiver.getUserId()) || conversation.getTutor().getUserId().equals(receiver.getUserId()))) {
            throw new RuntimeException("Receiver is not part of the conversation");
        }

        // Create and save the message
        MessageEntity message = new MessageEntity();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(messageDTO.getContent());
        message.setIsRead(false);

        // Set message type if provided
        if (messageDTO.getMessageType() != null) {
            message.setMessageType(messageDTO.getMessageType());
        }

        // Set session if sessionId is provided
        if (messageDTO.getSessionId() != null) {
            TutoringSessionEntity session = tutoringSessionRepository.findById(messageDTO.getSessionId())
                    .orElseThrow(() -> new RuntimeException("Session not found with ID: " + messageDTO.getSessionId()));
            message.setSession(session);
        }

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

    /**
     * Send a session details message in a conversation
     * @param conversationId The ID of the conversation
     * @param sessionId The ID of the tutoring session
     * @param senderId The ID of the sender (usually the system or the tutor)
     * @param receiverId The ID of the receiver
     * @return The created message
     * @throws RuntimeException if the conversation, session, sender, or receiver is not found
     */
    @Transactional
    public MessageEntity sendSessionDetailsMessage(Long conversationId, Long sessionId, Long senderId, Long receiverId) {
        System.out.println("Attempting to send session details message:");
        System.out.println("- Conversation ID: " + conversationId);
        System.out.println("- Session ID: " + sessionId);
        System.out.println("- Sender ID: " + senderId);
        System.out.println("- Receiver ID: " + receiverId);

        try {
            // Validate conversation
            ConversationEntity conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found with ID: " + conversationId));
            
            System.out.println("Found conversation between student=" + conversation.getStudent().getUserId() + 
                              " and tutor=" + conversation.getTutor().getUserId());

            // Validate session
            TutoringSessionEntity session = tutoringSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found with ID: " + sessionId));
            
            System.out.println("Found session with subject: " + session.getSubject() + 
                              ", student=" + session.getStudent().getUserId() + 
                              ", tutor=" + session.getTutor().getUserId());

            // Check if the session has a different tutor ID (profile ID vs user ID issue)
            if (!session.getTutor().getUserId().equals(conversation.getTutor().getUserId())) {
                System.out.println("WARNING: Mismatch between session tutor ID (" + session.getTutor().getUserId() + 
                                  ") and conversation tutor ID (" + conversation.getTutor().getUserId() + ")");
            }

            // Validate sender with better error handling
            UserEntity sender;
            try {
                sender = userRepository.findById(senderId)
                        .orElseThrow(() -> new RuntimeException("Sender not found with ID: " + senderId));
                System.out.println("Found sender: " + sender.getUsername() + " (ID: " + sender.getUserId() + ")");
            } catch (Exception e) {
                System.out.println("ERROR: Failed to find sender with ID " + senderId + ": " + e.getMessage());
                e.printStackTrace();
                throw e;
            }

            // Validate receiver with better error handling  
            UserEntity receiver;
            try {
                receiver = userRepository.findById(receiverId)
                        .orElseThrow(() -> new RuntimeException("Receiver not found with ID: " + receiverId));
                System.out.println("Found receiver: " + receiver.getUsername() + " (ID: " + receiver.getUserId() + ")");
            } catch (Exception e) {
                System.out.println("ERROR: Failed to find receiver with ID " + receiverId + ": " + e.getMessage());
                e.printStackTrace();
                throw e;
            }

            // Create session details content
            String content = formatSessionDetailsContent(session);

            // Create and save the message
            MessageEntity message = new MessageEntity();
            message.setConversation(conversation);
            message.setSender(sender);
            message.setReceiver(receiver);
            message.setContent(content);
            message.setIsRead(false);
            message.setMessageType(MessageEntity.MessageType.SESSION_DETAILS);
            message.setSession(session);

            MessageEntity savedMessage = messageRepository.save(message);
            System.out.println("Successfully saved session details message with ID: " + savedMessage.getMessageId());
            return savedMessage;
        } catch (Exception e) {
            System.out.println("ERROR in sendSessionDetailsMessage: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Send a session action message (accept/reject) in a conversation
     * @param conversationId The ID of the conversation
     * @param sessionId The ID of the tutoring session
     * @param senderId The ID of the sender (usually the tutor)
     * @param receiverId The ID of the receiver (usually the student)
     * @param accepted Whether the session was accepted or rejected
     * @return The created message
     * @throws RuntimeException if the conversation, session, sender, or receiver is not found
     */
    @Transactional
    public MessageEntity sendSessionActionMessage(Long conversationId, Long sessionId, Long senderId, Long receiverId, boolean accepted) {
        // Validate conversation
        ConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found with ID: " + conversationId));

        // Validate session
        TutoringSessionEntity session = tutoringSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with ID: " + sessionId));

        // Validate sender
        UserEntity sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found with ID: " + senderId));

        // Validate receiver
        UserEntity receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found with ID: " + receiverId));

        // Create action content
        String content = accepted ? 
                "Session request accepted. The tutoring session has been scheduled." : 
                "Session request declined. Please contact the tutor for more information.";

        // Create and save the message
        MessageEntity message = new MessageEntity();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        message.setIsRead(false);
        message.setMessageType(MessageEntity.MessageType.SESSION_ACTION);
        message.setSession(session);

        return messageRepository.save(message);
    }

    /**
     * Format session details into a readable message content
     * @param session The tutoring session
     * @return Formatted session details as a string
     */
    private String formatSessionDetailsContent(TutoringSessionEntity session) {
        StringBuilder content = new StringBuilder();
        content.append("**Tutoring Session Details**\n\n");
        content.append("Subject: ").append(session.getSubject()).append("\n");
        content.append("Date: ").append(formatDate(session.getStartTime())).append("\n");
        content.append("Time: ").append(formatTime(session.getStartTime())).append(" - ").append(formatTime(session.getEndTime())).append("\n");
        content.append("Status: ").append(session.getStatus()).append("\n");

        if (session.getPrice() != null) {
            content.append("Price: $").append(String.format("%.2f", session.getPrice())).append("\n");
        }

        if (session.getSessionType() != null) {
            content.append("Session Type: ").append(session.getSessionType()).append("\n");

            if ("online".equalsIgnoreCase(session.getSessionType()) && session.getMeetingLink() != null) {
                content.append("Meeting Link: ").append(session.getMeetingLink()).append("\n");
            } else if ("in-person".equalsIgnoreCase(session.getSessionType()) && session.getLocationData() != null) {
                content.append("Location: ").append(session.getLocationData()).append("\n");
            }
        }

        if (session.getNotes() != null && !session.getNotes().isEmpty()) {
            content.append("\nNotes: ").append(session.getNotes()).append("\n");
        }

        return content.toString();
    }

    /**
     * Format a date to a readable string
     * @param date The date to format
     * @return Formatted date string
     */
    private String formatDate(java.util.Date date) {
        if (date == null) return "Not specified";
        return new java.text.SimpleDateFormat("EEEE, MMMM d, yyyy").format(date);
    }

    /**
     * Format a time to a readable string
     * @param date The date/time to format
     * @return Formatted time string
     */
    private String formatTime(java.util.Date date) {
        if (date == null) return "Not specified";
        return new java.text.SimpleDateFormat("h:mm a").format(date);
    }
}
