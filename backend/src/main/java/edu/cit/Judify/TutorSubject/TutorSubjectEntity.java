package edu.cit.Judify.TutorSubject;

import edu.cit.Judify.TutorProfile.TutorProfileEntity;
import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "tutor_subjects")
public class TutorSubjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private TutorProfileEntity tutorProfile;

    @Column(nullable = false)
    private String subject;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    // Default constructor
    public TutorSubjectEntity() {
        this.createdAt = new Date();
    }

    // Constructor with fields
    public TutorSubjectEntity(TutorProfileEntity tutorProfile, String subject) {
        this.tutorProfile = tutorProfile;
        this.subject = subject;
        this.createdAt = new Date();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TutorProfileEntity getTutorProfile() {
        return tutorProfile;
    }

    public void setTutorProfile(TutorProfileEntity tutorProfile) {
        this.tutorProfile = tutorProfile;
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