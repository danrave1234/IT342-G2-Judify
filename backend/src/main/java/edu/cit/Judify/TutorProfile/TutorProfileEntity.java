package edu.cit.Judify.TutorProfile;

import edu.cit.Judify.User.UserEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "tutor_profiles")
public class TutorProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tutorProfileId;

    // One-to-one relationship with UserEntity
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(length = 1000)
    private String biography;
    private String certifications;
    private String subjectsExpertise; // Could be extended to a many-to-many relationship
    private Double hourlyRate;

    // Storing as JSON or String (you may later change this to a dedicated entity)
    @Column(length = 1000)
    private String availabilitySchedule;

    private Double averageRating;
    private Integer ratingsCount;
    private String location; // GPS coordinates

    // Constructors
    public TutorProfileEntity() {
    }

    // Getters and Setters

    public Long getTutorProfileId() {
        return tutorProfileId;
    }
    public void setTutorProfileId(Long tutorProfileId) {
        this.tutorProfileId = tutorProfileId;
    }

    public UserEntity getUser() {
        return user;
    }
    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getBiography() {
        return biography;
    }
    public void setBiography(String biography) {
        this.biography = biography;
    }

    public String getCertifications() {
        return certifications;
    }
    public void setCertifications(String certifications) {
        this.certifications = certifications;
    }

    public String getSubjectsExpertise() {
        return subjectsExpertise;
    }
    public void setSubjectsExpertise(String subjectsExpertise) {
        this.subjectsExpertise = subjectsExpertise;
    }

    public Double getHourlyRate() {
        return hourlyRate;
    }
    public void setHourlyRate(Double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public String getAvailabilitySchedule() {
        return availabilitySchedule;
    }
    public void setAvailabilitySchedule(String availabilitySchedule) {
        this.availabilitySchedule = availabilitySchedule;
    }

    public Double getAverageRating() {
        return averageRating;
    }
    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getRatingsCount() {
        return ratingsCount;
    }
    public void setRatingsCount(Integer ratingsCount) {
        this.ratingsCount = ratingsCount;
    }

    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }
}
