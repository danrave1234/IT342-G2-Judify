package edu.cit.Judify.WebSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

/**
 * Listens for WebSocket events such as connect and disconnect.
 * Used to clean up sessions when users disconnect suddenly.
 */
@Component
public class WebSocketEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);
    
    @Autowired
    private WebSocketSessionManager sessionManager;
    
    /**
     * Handle WebSocket connection events
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        logger.info("New WebSocket connection established: {}", sessionId);
    }
    
    /**
     * Handle WebSocket disconnection events
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        logger.info("WebSocket connection disconnected: {}", sessionId);
        
        // Find the user associated with this session and clean up their state
        // In a production app, you would store the user ID in the session attributes
        // when they connect and retrieve it here
        
        // This is a simplified implementation
        // In reality, you would need to map sessions to user IDs when they join
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null && sessionAttributes.containsKey("userId")) {
            Long userId = (Long) sessionAttributes.get("userId");
            
            // Clean up user's sessions
            sessionManager.removeUserCompletely(userId);
            
            logger.info("User {} disconnected, cleaned up their sessions", userId);
        }
    }
} 