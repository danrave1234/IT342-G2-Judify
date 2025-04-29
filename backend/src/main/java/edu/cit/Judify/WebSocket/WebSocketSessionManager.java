package edu.cit.Judify.WebSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages WebSocket sessions and tracks which users are connected to which conversations.
 * This ensures messages are only sent to users who have the conversation open.
 */
@Component
public class WebSocketSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketSessionManager.class);
    
    // Map of userId -> set of conversationIds the user is currently connected to
    private final Map<Long, Set<Long>> userConversations = new ConcurrentHashMap<>();
    
    // Map of userId -> sessionId for tracking active user sessions
    private final Map<Long, String> userSessions = new ConcurrentHashMap<>();
    
    // Map of conversationId -> set of userIds currently viewing the conversation
    private final Map<Long, Set<Long>> conversationUsers = new ConcurrentHashMap<>();
    
    /**
     * Register a user as connected to a specific conversation
     */
    public void registerUserSession(Long userId, Long conversationId, String sessionId) {
        // Track the user's session
        userSessions.put(userId, sessionId);
        
        // Add this conversation to the user's active conversations
        userConversations.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(conversationId);
        
        // Add this user to the conversation's active users
        conversationUsers.computeIfAbsent(conversationId, k -> ConcurrentHashMap.newKeySet()).add(userId);
        
        logger.info("User {} registered for conversation {} with session {}", userId, conversationId, sessionId);
    }
    
    /**
     * Remove a user from a specific conversation
     */
    public void removeUserSession(Long userId, Long conversationId) {
        // Remove this conversation from the user's active conversations
        Set<Long> userConvs = userConversations.get(userId);
        if (userConvs != null) {
            userConvs.remove(conversationId);
            if (userConvs.isEmpty()) {
                userConversations.remove(userId);
                userSessions.remove(userId);
            }
        }
        
        // Remove this user from the conversation's active users
        Set<Long> convUsers = conversationUsers.get(conversationId);
        if (convUsers != null) {
            convUsers.remove(userId);
            if (convUsers.isEmpty()) {
                conversationUsers.remove(conversationId);
            }
        }
        
        logger.info("User {} unregistered from conversation {}", userId, conversationId);
    }
    
    /**
     * Check if a user is connected to a specific conversation
     */
    public boolean isUserConnectedToConversation(Long userId, Long conversationId) {
        Set<Long> userConvs = userConversations.get(userId);
        return userConvs != null && userConvs.contains(conversationId);
    }
    
    /**
     * Check if a user is connected to any conversation
     */
    public boolean isUserConnected(Long userId) {
        return userSessions.containsKey(userId);
    }
    
    /**
     * Get all user IDs connected to a specific conversation
     */
    public Set<Long> getUsersInConversation(Long conversationId) {
        return conversationUsers.getOrDefault(conversationId, ConcurrentHashMap.newKeySet());
    }
    
    /**
     * Get all conversation IDs a user is connected to
     */
    public Set<Long> getUserConversations(Long userId) {
        return userConversations.getOrDefault(userId, ConcurrentHashMap.newKeySet());
    }
    
    /**
     * Get the session ID for a specific user
     */
    public String getUserSessionId(Long userId) {
        return userSessions.get(userId);
    }
    
    /**
     * Clean up a user's sessions when they disconnect completely
     */
    public void removeUserCompletely(Long userId) {
        // Get all conversations the user is in
        Set<Long> userConvs = userConversations.get(userId);
        if (userConvs != null) {
            // Remove user from each conversation's active users
            for (Long conversationId : userConvs) {
                Set<Long> convUsers = conversationUsers.get(conversationId);
                if (convUsers != null) {
                    convUsers.remove(userId);
                    if (convUsers.isEmpty()) {
                        conversationUsers.remove(conversationId);
                    }
                }
            }
            
            // Remove user completely
            userConversations.remove(userId);
            userSessions.remove(userId);
        }
        
        logger.info("User {} completely removed from all conversations", userId);
    }
} 