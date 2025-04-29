package edu.cit.Judify.WebSocket;

import edu.cit.Judify.Conversation.ConversationEntity;
import edu.cit.Judify.Conversation.ConversationService;
import edu.cit.Judify.Message.MessageEntity;
import edu.cit.Judify.Message.MessageService;
import edu.cit.Judify.User.UserEntity;
import edu.cit.Judify.User.UserService;
import edu.cit.Judify.Message.DTO.MessageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class WebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private WebSocketSessionManager sessionManager;
    
    /**
     * Utility method to find a conversation by its ID, supporting both numeric IDs and string IDs
     * @param conversationIdStr The conversation ID as a string
     * @return Optional containing the conversation if found
     */
    private Optional<ConversationEntity> findConversation(String conversationIdStr) {
        try {
            // First try to parse as a Long (database ID)
            Long conversationId = Long.parseLong(conversationIdStr);
            return conversationService.getConversationById(conversationId);
        } catch (NumberFormatException e) {
            // If it's not a numeric ID, it might be a client-generated ID (e.g., conv_timestamp_randomstring)
            logger.info("Conversation ID '{}' is not numeric, looking up by string identifier", conversationIdStr);
            
            // For now, just create a new temporary conversation for string IDs
            // In a real implementation, you would have a way to look up conversations by string IDs too
            return Optional.empty();
        }
    }
    
    /**
     * Utility method to register a conversation session regardless of ID type
     */
    private void registerConversationSession(Long userId, String conversationIdStr, String sessionId) {
        try {
            Long conversationId = Long.parseLong(conversationIdStr);
            sessionManager.registerUserSession(userId, conversationId, sessionId);
        } catch (NumberFormatException e) {
            // For string IDs, we'll use a hash code as the numeric ID for the session manager
            Long conversationIdHash = (long) conversationIdStr.hashCode();
            sessionManager.registerUserSession(userId, conversationIdHash, sessionId);
        }
    }
    
    /**
     * Utility method to check if a user is connected to a conversation, regardless of ID type
     */
    private boolean isUserConnectedToConversation(Long userId, String conversationIdStr) {
        try {
            Long conversationId = Long.parseLong(conversationIdStr);
            return sessionManager.isUserConnectedToConversation(userId, conversationId);
        } catch (NumberFormatException e) {
            // For string IDs, use the hash code
            Long conversationIdHash = (long) conversationIdStr.hashCode();
            return sessionManager.isUserConnectedToConversation(userId, conversationIdHash);
        }
    }

    /**
     * Handles WebSocket connection when a user joins a specific conversation.
     * This is called when a user opens a conversation in the UI.
     */
    @MessageMapping("/chat.join/{conversationId}")
    public void joinConversation(
            @DestinationVariable String conversationId,
            @Payload ChatMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        
        logger.info("User {} joining conversation {}", message.getSenderId(), conversationId);
        
        // Get the user that's joining
        Optional<UserEntity> userOpt = userService.getUserById(message.getSenderId());
        if (!userOpt.isPresent()) {
            logger.error("User {} not found when joining conversation {}", message.getSenderId(), conversationId);
            return;
        }
        
        // Get the conversation - support both numeric and string IDs
        Optional<ConversationEntity> conversationOpt = findConversation(conversationId);
        if (!conversationOpt.isPresent()) {
            // For client-generated IDs, we don't have a corresponding database entry yet
            // Just register the session for now
            logger.info("Conversation {} not found in database but proceeding with client-side ID", conversationId);
            
            // Store user's session for this conversation using hash code for string IDs
            String sessionId = headerAccessor.getSessionId();
            registerConversationSession(message.getSenderId(), conversationId, sessionId);
            return;
        }
        
        // Store user's session for this conversation
        String sessionId = headerAccessor.getSessionId();
        registerConversationSession(message.getSenderId(), conversationId, sessionId);
        
        logger.info("User {} successfully joined conversation {}", message.getSenderId(), conversationId);
        
        // For existing database conversations, notify other participants
        ConversationEntity conversation = conversationOpt.get();
        UserEntity sender = userOpt.get();
        
        // Get both participants from the conversation (student and tutor)
        List<UserEntity> participants = new ArrayList<>();
        participants.add(conversation.getStudent());
        participants.add(conversation.getTutor());
        
        participants.stream()
            .filter(user -> !user.getUserId().equals(sender.getUserId()))
            .findFirst()
            .ifPresent(receiver -> {
                // Only send the notification if the other user is connected to this conversation
                if (isUserConnectedToConversation(receiver.getUserId(), conversationId)) {
                    ChatMessage joinMessage = new ChatMessage();
                    joinMessage.setType(ChatMessage.MessageType.JOIN);
                    joinMessage.setSenderId(sender.getUserId());
                    joinMessage.setConversationId(conversation.getConversationId());
                    joinMessage.setContent(sender.getFirstName() + " " + sender.getLastName() + " joined the conversation");
                    
                    messagingTemplate.convertAndSendToUser(
                        receiver.getUserId().toString(),
                        "/queue/messages",
                        joinMessage
                    );
                }
            });
    }

    /**
     * Handles WebSocket disconnection when a user leaves a conversation.
     * This is called when a user closes a conversation or navigates away.
     */
    @MessageMapping("/chat.leave/{conversationId}")
    public void leaveConversation(
            @DestinationVariable String conversationId,
            @Payload ChatMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        
        logger.info("User {} leaving conversation {}", message.getSenderId(), conversationId);
        
        // For string IDs, use the hash code
        Long conversationIdHash = null;
        try {
            conversationIdHash = Long.parseLong(conversationId);
        } catch (NumberFormatException e) {
            conversationIdHash = (long) conversationId.hashCode();
        }
        
        // Remove user's session for this conversation
        sessionManager.removeUserSession(message.getSenderId(), conversationIdHash);
        
        // Notify the other user in the conversation that this user has left, if they're connected
        Optional<UserEntity> userOpt = userService.getUserById(message.getSenderId());
        Optional<ConversationEntity> conversationOpt = findConversation(conversationId);
        
        if (userOpt.isPresent() && conversationOpt.isPresent()) {
            UserEntity sender = userOpt.get();
            ConversationEntity conversation = conversationOpt.get();
            
            // Get both participants from the conversation (student and tutor)
            List<UserEntity> participants = new ArrayList<>();
            participants.add(conversation.getStudent());
            participants.add(conversation.getTutor());
            
            participants.stream()
                .filter(user -> !user.getUserId().equals(sender.getUserId()))
                .findFirst()
                .ifPresent(receiver -> {
                    // Only send the notification if the other user is connected to this conversation
                    if (isUserConnectedToConversation(receiver.getUserId(), conversationId)) {
                        ChatMessage leaveMessage = new ChatMessage();
                        leaveMessage.setType(ChatMessage.MessageType.LEAVE);
                        leaveMessage.setSenderId(sender.getUserId());
                        leaveMessage.setConversationId(conversation.getConversationId());
                        leaveMessage.setContent(sender.getFirstName() + " " + sender.getLastName() + " left the conversation");
                        
                        messagingTemplate.convertAndSendToUser(
                            receiver.getUserId().toString(),
                            "/queue/messages",
                            leaveMessage
                        );
                    }
                });
        }
    }

    /**
     * Handles sending a message in a conversation.
     * For client-side conversation IDs, we'll store messages in memory for the session.
     */
    @MessageMapping("/chat.send/{conversationId}")
    public void sendMessage(
            @DestinationVariable String conversationId,
            @Payload ChatMessage chatMessage) {
        
        logger.info("Received message from user {} to conversation {}", chatMessage.getSenderId(), conversationId);
        
        try {
            // Validate the sender and receiver
            Optional<UserEntity> senderOpt = userService.getUserById(chatMessage.getSenderId());
            Optional<UserEntity> receiverOpt = userService.getUserById(chatMessage.getReceiverId());
            
            if (!senderOpt.isPresent() || !receiverOpt.isPresent()) {
                logger.error("Invalid sender or receiver for message");
                return;
            }
            
            UserEntity sender = senderOpt.get();
            UserEntity receiver = receiverOpt.get();

            // Get or create the conversation
            Optional<ConversationEntity> conversationOpt = findConversation(conversationId);
            MessageEntity messageEntity = null;
            
            if (conversationOpt.isPresent()) {
                // Database conversation exists, create and save message
                ConversationEntity conversation = conversationOpt.get();
                
                // Create message entity
                messageEntity = new MessageEntity();
                messageEntity.setConversation(conversation);
                messageEntity.setSender(sender);
                messageEntity.setReceiver(receiver);
                messageEntity.setContent(chatMessage.getContent());
                messageEntity.setIsRead(false);
                
                // Save to database using the MessageService from the latest implementation
                MessageDTO msgDTO = new MessageDTO();
                msgDTO.setConversationId(conversation.getConversationId());
                msgDTO.setSenderId(sender.getUserId());
                msgDTO.setReceiverId(receiver.getUserId());
                msgDTO.setContent(chatMessage.getContent());
                messageEntity = messageService.sendMessage(msgDTO);
                
                // Update the chat message with the saved message ID
                chatMessage.setMessageId(messageEntity.getMessageId().toString());
                chatMessage.setTimestamp(messageEntity.getTimestamp());
                chatMessage.setIsRead(messageEntity.getIsRead());
            } else {
                // Client-side conversation, just use the message as is
                logger.info("Using client-side conversation ID: {}", conversationId);
                // The messageId and timestamp were set by the client
            }
            
            // Send the message to the receiver if they are connected to this conversation
            if (isUserConnectedToConversation(receiver.getUserId(), conversationId)) {
                logger.info("Sending message to user {}", receiver.getUserId());
                messagingTemplate.convertAndSendToUser(
                    receiver.getUserId().toString(),
                    "/queue/messages",
                    chatMessage
                );
            } else {
                logger.info("Receiver {} is not connected to conversation {}, message is stored only", 
                          receiver.getUserId(), conversationId);
            }
            
            // Send acknowledgment back to the sender
            messagingTemplate.convertAndSendToUser(
                sender.getUserId().toString(),
                "/queue/messages",
                chatMessage
            );
            
        } catch (Exception e) {
            logger.error("Error processing message: ", e);
        }
    }

    /**
     * Handles marking a message as read.
     */
    @MessageMapping("/chat.read/{messageId}")
    public void markMessageAsRead(
            @DestinationVariable String messageId,
            @Payload ChatMessage readReceipt) {
        
        logger.info("Marking message {} as read by user {}", messageId, readReceipt.getReceiverId());
        
        try {
            Long messageIdLong = null;
            
            // Try to parse the messageId as Long (for database messages)
            try {
                messageIdLong = Long.parseLong(messageId);
            } catch (NumberFormatException e) {
                // This might be a client-generated message ID, just notify the sender
                logger.info("Message ID {} is not numeric, skipping database update", messageId);
            }
            
            MessageEntity message = null;
            if (messageIdLong != null) {
                // Mark the message as read in the database
                message = messageService.markMessageAsRead(messageIdLong);
            }
            
            // Determine the sender to notify
            Long senderId = readReceipt.getSenderId();
            
            // Check if we should use the message from the database
            if (message != null) {
                // Use the sender from the database message
                senderId = message.getSender().getUserId();
            }
            
            // Notify the sender that their message has been read
            if (senderId != null && sessionManager.isUserConnected(senderId)) {
                ChatMessage receipt = new ChatMessage();
                receipt.setMessageId(messageId);
                receipt.setSenderId(senderId);
                receipt.setReceiverId(readReceipt.getReceiverId());
                receipt.setConversationId(readReceipt.getConversationId());
                receipt.setIsRead(true);
                
                messagingTemplate.convertAndSendToUser(
                    senderId.toString(),
                    "/queue/receipts",
                    receipt
                );
            }
        } catch (Exception e) {
            logger.error("Error marking message as read: ", e);
        }
    }
} 