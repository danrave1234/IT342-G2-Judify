package edu.cit.Judify.WebSocket;

import java.util.Date;

public class ChatMessage {
    
    private String messageId;
    private Long senderId;
    private Long receiverId;
    private Long conversationId;
    private String content;
    private Date timestamp;
    private Boolean isRead;
    private MessageType type;
    
    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE
    }
    
    // Default constructor
    public ChatMessage() {
        this.timestamp = new Date();
        this.isRead = false;
    }
    
    // Constructor with parameters
    public ChatMessage(String messageId, Long senderId, Long receiverId, Long conversationId,
                      String content, MessageType type) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.conversationId = conversationId;
        this.content = content;
        this.timestamp = new Date();
        this.isRead = false;
        this.type = type;
    }
    
    // Getters and setters
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public Long getSenderId() {
        return senderId;
    }
    
    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }
    
    public Long getReceiverId() {
        return receiverId;
    }
    
    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }
    
    public Long getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    
    public Boolean getIsRead() {
        return isRead;
    }
    
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
    
    public MessageType getType() {
        return type;
    }
    
    public void setType(MessageType type) {
        this.type = type;
    }
} 