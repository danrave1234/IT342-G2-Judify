package edu.cit.Storix.Message;

import edu.cit.Storix.Conversation.ConversationEntity;
import edu.cit.Storix.User.UserEntity;
import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "messages")
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    @ManyToOne
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;

    @Column(nullable = false, length = 2000)
    private String content;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date timestamp;

    @Column(nullable = false)
    private Boolean isRead;

    // Constructors
    public MessageEntity() {
    }

    // Getters and Setters

    public Long getMessageId() {
        return messageId;
    }
    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public ConversationEntity getConversation() {
        return conversation;
    }
    public void setConversation(ConversationEntity conversation) {
        this.conversation = conversation;
    }

    public UserEntity getSender() {
        return sender;
    }
    public void setSender(UserEntity sender) {
        this.sender = sender;
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

    @PrePersist
    protected void onCreate() {
        timestamp = new Date();
        if (isRead == null) {
            isRead = false;
        }
    }
}
