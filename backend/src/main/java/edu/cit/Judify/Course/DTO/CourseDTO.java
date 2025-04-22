package edu.cit.Judify.Course.DTO;

import java.util.Date;

/**
 * Data Transfer Object for Course
 */
public class CourseDTO {
    
    private Long id;
    private String title;
    private String subtitle;
    private String description;
    private Long tutorId;
    private String tutorName;
    private String category;
    private Double price;
    private Date createdAt;
    
    // Default constructor
    public CourseDTO() {
    }
    
    // Constructor with all fields
    public CourseDTO(Long id, String title, String subtitle, String description, 
                    Long tutorId, String tutorName, String category, Double price, Date createdAt) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.description = description;
        this.tutorId = tutorId;
        this.tutorName = tutorName;
        this.category = category;
        this.price = price;
        this.createdAt = createdAt;
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
    
    public Long getTutorId() {
        return tutorId;
    }
    
    public void setTutorId(Long tutorId) {
        this.tutorId = tutorId;
    }
    
    public String getTutorName() {
        return tutorName;
    }
    
    public void setTutorName(String tutorName) {
        this.tutorName = tutorName;
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