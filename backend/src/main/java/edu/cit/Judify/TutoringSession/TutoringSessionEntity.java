package edu.cit.Judify.TutoringSession;

import edu.cit.Judify.User.UserEntity;
import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "tutoring_sessions")
public class TutoringSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sessionId;

    // Many sessions can be linked to a tutor and a learner (both are UserEntity)
    @ManyToOne
    @JoinColumn(name = "tutor_id", nullable = false)
    private UserEntity tutor;

    @ManyToOne
    @JoinColumn(name = "learner_id", nullable = false)
    private UserEntity learner;

    @Temporal(TemporalType.TIMESTAMP)
    private Date scheduledStart;

    @Temporal(TemporalType.TIMESTAMP)
    private Date scheduledEnd;

    @Column(nullable = false)
    private String status; // e.g., pending, confirmed, completed, cancelled

    private String meetingLink;    // For online sessions
    private String locationData;   // For in-person sessions

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false, updatable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    // Constructors
    public TutoringSessionEntity() {
    }

    // Getters and Setters

    public Long getSessionId() {
        return sessionId;
    }
    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public UserEntity getTutor() {
        return tutor;
    }
    public void setTutor(UserEntity tutor) {
        this.tutor = tutor;
    }

    public UserEntity getLearner() {
        return learner;
    }
    public void setLearner(UserEntity learner) {
        this.learner = learner;
    }

    public Date getScheduledStart() {
        return scheduledStart;
    }
    public void setScheduledStart(Date scheduledStart) {
        this.scheduledStart = scheduledStart;
    }

    public Date getScheduledEnd() {
        return scheduledEnd;
    }
    public void setScheduledEnd(Date scheduledEnd) {
        this.scheduledEnd = scheduledEnd;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public String getMeetingLink() {
        return meetingLink;
    }
    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }

    public String getLocationData() {
        return locationData;
    }
    public void setLocationData(String locationData) {
        this.locationData = locationData;
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
