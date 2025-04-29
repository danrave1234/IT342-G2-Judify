package edu.cit.Judify.TutoringSession;

import java.util.Date;

import edu.cit.Judify.Conversation.ConversationEntity;
import edu.cit.Judify.User.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

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
    @JoinColumn(name = "student_id", nullable = false)
    private UserEntity student;

    @Temporal(TemporalType.TIMESTAMP)
    private Date startTime;

    @Temporal(TemporalType.TIMESTAMP)
    private Date endTime;

    private String subject;
    private String status;
    private Double price;
    private String notes;

    @Column(nullable = true)    // Explicitly marking as nullable
    private String locationData;    // For in-person sessions

    @Column(nullable = true)    // Explicitly marking as nullable
    private String meetingLink;     // For online sessions

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false, updatable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Column(nullable = true)
    private Boolean tutorAccepted;

    @Column(nullable = true)
    private Boolean studentAccepted;

    @OneToOne
    @JoinColumn(name = "conversation_id", nullable = true)
    private ConversationEntity conversation;

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

    public UserEntity getStudent() {
        return student;
    }
    public void setStudent(UserEntity student) {
        this.student = student;
    }

    public Date getStartTime() {
        return startTime;
    }
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }
    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getSubject() {
        return subject;
    }
    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public Double getPrice() {
        return price;
    }
    public void setPrice(Double price) {
        this.price = price;
    }

    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getLocationData() {
        return locationData;
    }
    public void setLocationData(String locationData) {
        this.locationData = locationData;
    }

    public String getMeetingLink() {
        return meetingLink;
    }
    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
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

    public Boolean getTutorAccepted() {
        return tutorAccepted;
    }

    public void setTutorAccepted(Boolean tutorAccepted) {
        this.tutorAccepted = tutorAccepted;
    }

    public Boolean getStudentAccepted() {
        return studentAccepted;
    }

    public void setStudentAccepted(Boolean studentAccepted) {
        this.studentAccepted = studentAccepted;
    }

    public ConversationEntity getConversation() {
        return conversation;
    }

    public void setConversation(ConversationEntity conversation) {
        this.conversation = conversation;
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
