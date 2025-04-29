package edu.cit.Judify.TutorProfile.DTO;

import java.util.Set;

/**
 * DTO for registering a new user as a tutor.
 * Combines user registration information with tutor profile information.
 */
public class TutorRegistrationDTO {
    // User information
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String contactDetails;
    
    // Tutor profile information
    private String bio;
    private String expertise;
    private Double hourlyRate;
    private Set<String> subjects;
    private Double latitude;
    private Double longitude;
    
    // Default constructor
    public TutorRegistrationDTO() {
    }
    
    // Getters and Setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
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
    
    public String getContactDetails() {
        return contactDetails;
    }
    
    public void setContactDetails(String contactDetails) {
        this.contactDetails = contactDetails;
    }
    
    public String getBio() {
        return bio;
    }
    
    public void setBio(String bio) {
        this.bio = bio;
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
    
    public Set<String> getSubjects() {
        return subjects;
    }
    
    public void setSubjects(Set<String> subjects) {
        this.subjects = subjects;
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
}