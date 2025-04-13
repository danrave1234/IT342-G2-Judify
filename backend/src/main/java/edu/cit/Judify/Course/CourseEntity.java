package edu.cit.Judify.Course;

import edu.cit.Judify.TutorProfile.TutorProfileEntity;
import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "courses")
public class CourseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String subtitle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "tutor_id", nullable = false)
    private TutorProfileEntity tutor;

    private String category;

    private Double price;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    // Default constructor
    public CourseEntity() {
        this.createdAt = new Date();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TutorProfileEntity getTutor() {
        return tutor;
    }

    public void setTutor(TutorProfileEntity tutor) {
        this.tutor = tutor;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}