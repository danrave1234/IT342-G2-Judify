package edu.cit.Judify.TutoringSession.DTO;

import java.util.Date;

public class TutoringSessionDTO {
    private Long sessionId;
    private Long userId;
    private Long studentId;
    private Date startTime;
    private Date endTime;
    private String subject;
    private String status; // PENDING, NEGOTIATING, SCHEDULED, ONGOING, COMPLETED, CANCELLED
    private Double price;
    private String notes;
    private Double latitude;       // Latitude for in-person sessions
    private Double longitude;      // Longitude for in-person sessions
    private String locationName;   // Location name for in-person sessions
    private String locationData;   // For backward compatibility
    private String meetingLink;     // For online sessions
    private String sessionType;     // "online" or "in-person"
    private Date createdAt;
    private Date updatedAt;
    private String tutorName;       // Added tutorName field
    private String studentName;     // Added studentName field
    private Boolean tutorAccepted;  // Whether the tutor has accepted the session
    private Boolean studentAccepted; // Whether the student has accepted the session
    private Long conversationId;    // ID of the conversation for negotiation

    // Default constructor
    public TutoringSessionDTO() {
    }

    // Getters and Setters
    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
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

    public String getTutorName() {
        return tutorName;
    }

    public void setTutorName(String tutorName) {
        this.tutorName = tutorName;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
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

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }
}
