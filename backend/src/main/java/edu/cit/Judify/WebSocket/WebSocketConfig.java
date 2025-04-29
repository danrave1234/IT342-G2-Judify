package edu.cit.Judify.WebSocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker for sending messages to clients
        // Messages with destination prefix "/topic" or "/queue" will be routed to the broker
        config.enableSimpleBroker("/topic", "/queue");
        
        // Messages with destination prefix "/app" will be routed to message handling methods
        config.setApplicationDestinationPrefixes("/app");
        
        // Set prefix for user-specific destinations
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the "/ws" endpoint, enabling SockJS fallback options so that
        // WebSocket works on browsers that don't support it
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // For dev environment, restrict in production
                .withSockJS();
    }
} 