package edu.cit.Judify.TutorSubject.DTO;

import java.util.Date;

public class TutorSubjectDTO {
    private Long id;
    private Long tutorProfileId;
    private String subject;
    private Date createdAt;

    // Default constructor
    public TutorSubjectDTO() {
    }

    // Constructor with fields
    public TutorSubjectDTO(Long id, Long tutorProfileId, String subject, Date createdAt) {
        this.id = id;
        this.tutorProfileId = tutorProfileId;
        this.subject = subject;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTutorProfileId() {
        return tutorProfileId;
    }

    public void setTutorProfileId(Long tutorProfileId) {
        this.tutorProfileId = tutorProfileId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
} 