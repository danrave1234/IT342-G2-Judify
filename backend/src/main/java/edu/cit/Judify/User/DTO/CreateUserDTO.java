package edu.cit.Judify.User.DTO;

import edu.cit.Judify.User.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

public class CreateUserDTO {
    
    @Schema(description = "User's email address")
    private String email;
    
    @Schema(description = "User's username")
    private String username;
    
    @Schema(description = "User's password")
    private String password;
    
    @Schema(description = "User's first name")
    private String firstName;
    
    @Schema(description = "User's last name")
    private String lastName;
    
    @Schema(description = "User's contact details")
    private String contactDetails;
    
    @Schema(description = "User's role", example = "STUDENT", defaultValue = "STUDENT")
    private UserRole role = UserRole.STUDENT;
    
    // Default constructor
    public CreateUserDTO() {
    }
    
    // Getters and setters
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
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
    
    public UserRole getRole() {
        return role;
    }
    
    public void setRole(UserRole role) {
        this.role = role;
    }
} 