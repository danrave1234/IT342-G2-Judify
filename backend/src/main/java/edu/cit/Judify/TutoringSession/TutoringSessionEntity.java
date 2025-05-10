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

    @Column(nullable = true)    // Latitude for in-person sessions
    private Double latitude;

    @Column(nullable = true)    // Longitude for in-person sessions
    private Double longitude;

    @Column(nullable = true)    // Location name for in-person sessions
    private String locationName;

    @Column(nullable = true)    // Explicitly marking as nullable
    private String meetingLink;     // For online sessions

    @Column(nullable = true)
    private String sessionType;    // "online" or "in-person"

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

    public Double getLatitude() {
        return latitude;
    }
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getLocationName() {
        return locationName;
    }
    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    // For backward compatibility
    public String getLocationData() {
        if (latitude != null && longitude != null) {
            String name = locationName != null ? locationName : "Location";
            return String.format("Lat: %f, Long: %f, Name: %s", latitude, longitude, name);
        }
        return null;
    }

    public void setLocationData(String locationData) {
        if (locationData != null && !locationData.isEmpty()) {
            // Try to parse latitude, longitude, and name from the locationData string
            try {
                // Expected format: "Lat: 12.345, Long: 67.890, Name: Some Location"
                String[] parts = locationData.split(",");
                if (parts.length >= 2) {
                    // Extract latitude
                    String latPart = parts[0].trim();
                    if (latPart.startsWith("Lat:")) {
                        this.latitude = Double.parseDouble(latPart.substring(4).trim());
                    }

                    // Extract longitude
                    String longPart = parts[1].trim();
                    if (longPart.startsWith("Long:")) {
                        this.longitude = Double.parseDouble(longPart.substring(5).trim());
                    }

                    // Extract name if available
                    if (parts.length >= 3) {
                        String namePart = parts[2].trim();
                        if (namePart.startsWith("Name:")) {
                            this.locationName = namePart.substring(5).trim();
                        }
                    }
                }
            } catch (Exception e) {
                // If parsing fails, store the whole string as locationName
                this.locationName = locationData;
            }
        } else {
            this.latitude = null;
            this.longitude = null;
            this.locationName = null;
        }
    }

    public String getMeetingLink() {
        return meetingLink;
    }
    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }

    public String getSessionType() {
        return sessionType;
    }
    public void setSessionType(String sessionType) {
        this.sessionType = sessionType;
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
