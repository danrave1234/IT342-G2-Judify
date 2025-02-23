package edu.cit.Storix.User;

import edu.cit.Storix.TutorProfile.TutorProfileEntity;
import jakarta.persistence.*;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String profilePicture;
    private String contactDetails;

    // Roles stored as a comma-separated string (e.g., "LEARNER,TUTOR,ADMIN")
    @Column(nullable = false)
    private String roles;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false, updatable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    // One-to-One relationship with TutorProfileEntity (optional)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private TutorProfileEntity tutorProfile;

    // Constructors
    public UserEntity() {
    }

    // Getters and Setters

    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getProfilePicture() {
        return profilePicture;
    }
    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getContactDetails() {
        return contactDetails;
    }
    public void setContactDetails(String contactDetails) {
        this.contactDetails = contactDetails;
    }

    public String getRoles() {
        return roles;
    }
    public void setRoles(String roles) {
        this.roles = roles;
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

    public TutorProfileEntity getTutorProfile() {
        return tutorProfile;
    }
    public void setTutorProfile(TutorProfileEntity tutorProfile) {
        this.tutorProfile = tutorProfile;
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
