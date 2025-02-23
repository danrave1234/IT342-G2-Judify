package edu.cit.Judify.Notification;

import edu.cit.Judify.User.UserEntity;
import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "notifications")
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private String type;  // e.g., session update, payment confirmation

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private Boolean isRead;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false, updatable = false)
    private Date createdAt;

    // Constructors
    public NotificationEntity() {
    }

    // Getters and Setters

    public Long getNotificationId() {
        return notificationId;
    }
    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public UserEntity getUser() {
        return user;
    }
    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getIsRead() {
        return isRead;
    }
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public Date getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        if (isRead == null) {
            isRead = false;
        }
    }
}
