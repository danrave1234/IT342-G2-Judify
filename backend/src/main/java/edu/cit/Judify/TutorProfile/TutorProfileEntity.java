package edu.cit.Judify.TutorProfile;

import edu.cit.Judify.Course.CourseEntity;
import edu.cit.Judify.TutorSubject.TutorSubjectEntity;
import edu.cit.Judify.User.UserEntity;
import jakarta.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tutor_profiles")
public class TutorProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One-to-one relationship with UserEntity
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(columnDefinition = "TEXT")
    private String biography;

    private String expertise;

    private Double hourlyRate;

    @OneToMany(mappedBy = "tutorProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TutorSubjectEntity> subjectEntities = new HashSet<>();

    private Double rating;

    private Integer totalReviews;

    private Double latitude;

    private Double longitude;

    @OneToMany(mappedBy = "tutor", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CourseEntity> courses = new HashSet<>();

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    // Default constructor
    public TutorProfileEntity() {
        this.createdAt = new Date();
        this.rating = 0.0;
        this.totalReviews = 0;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getExpertise() {
        return expertise;
    }

    public void setExpertise(String expertise) {
        this.expertise = expertise;
    }

    public Double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(Double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public Set<TutorSubjectEntity> getSubjectEntities() {
        return subjectEntities;
    }

    public void setSubjectEntities(Set<TutorSubjectEntity> subjectEntities) {
        this.subjectEntities = subjectEntities;
    }

    // Helper method to maintain backward compatibility
    public Set<String> getSubjects() {
        Set<String> subjects = new HashSet<>();
        for (TutorSubjectEntity entity : subjectEntities) {
            subjects.add(entity.getSubject());
        }
        return subjects;
    }

    // Helper method to maintain backward compatibility
    public void setSubjects(Set<String> subjects) {
        // Clear existing subjects
        this.subjectEntities.clear();

        // Add new subjects
        if (subjects != null) {
            for (String subject : subjects) {
                TutorSubjectEntity subjectEntity = new TutorSubjectEntity(this, subject);
                this.subjectEntities.add(subjectEntity);
            }
        }
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(Integer totalReviews) {
        this.totalReviews = totalReviews;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
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

    public Set<CourseEntity> getCourses() {
        return courses;
    }

    public void setCourses(Set<CourseEntity> courses) {
        this.courses = courses;
    }

    // Helper method to add a course
    public void addCourse(CourseEntity course) {
        courses.add(course);
        course.setTutor(this);
    }

    // Helper method to remove a course
    public void removeCourse(CourseEntity course) {
        courses.remove(course);
        course.setTutor(null);
    }
}
