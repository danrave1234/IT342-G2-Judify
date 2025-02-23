package edu.cit.Storix.Conversation;

import edu.cit.Storix.User.UserEntity;
import jakarta.persistence.*;

import java.util.Date;
import java.util.Set;

@Entity
@Table(name = "conversations")
public class ConversationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long conversationId;

    // For simplicity, using a ManyToMany relationship with users
    @ManyToMany
    @JoinTable(
            name = "conversation_participants",
            joinColumns = @JoinColumn(name = "conversation_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<UserEntity> participants;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false, updatable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    // Constructors
    public ConversationEntity() {
    }

    // Getters and Setters

    public Long getConversationId() {
        return conversationId;
    }
    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Set<UserEntity> getParticipants() {
        return participants;
    }
    public void setParticipants(Set<UserEntity> participants) {
        this.participants = participants;
    }

    public Date getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
}
