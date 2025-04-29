package edu.cit.Judify.StudentProfile;

import edu.cit.Judify.StudentProfile.DTO.CreateStudentProfileRequest;
import edu.cit.Judify.StudentProfile.DTO.StudentProfileDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/student-profiles")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    @Autowired
    public StudentProfileController(StudentProfileService studentProfileService) {
        this.studentProfileService = studentProfileService;
    }

    /**
     * Get a student profile by ID
     * @param id Profile ID
     * @return The profile data
     */
    @GetMapping("/{id}")
    public ResponseEntity<StudentProfileDTO> getProfileById(@PathVariable Long id) {
        try {
            StudentProfileDTO profile = studentProfileService.findById(id);
            return ResponseEntity.ok(profile);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving student profile: " + e.getMessage());
        }
    }

    /**
     * Get a student profile by user ID
     * @param userId User ID
     * @return The profile data
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<StudentProfileDTO> getProfileByUserId(@PathVariable Long userId) {
        try {
            StudentProfileDTO profile = studentProfileService.findByUserId(userId);
            return ResponseEntity.ok(profile);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving student profile: " + e.getMessage());
        }
    }

    /**
     * Create a new student profile
     * @param request Profile creation request
     * @return The created profile
     */
    @PostMapping
    public ResponseEntity<StudentProfileDTO> createProfile(@RequestBody CreateStudentProfileRequest request) {
        try {
            // Validate request
            if (request.getUserId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID is required");
            }

            // Check if profile already exists
            if (studentProfileService.existsByUserId(request.getUserId())) {
                return updateProfile(request.getUserId(), request);
            }

            StudentProfileDTO createdProfile = studentProfileService.createProfile(request);
            return new ResponseEntity<>(createdProfile, HttpStatus.CREATED);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating student profile: " + e.getMessage());
        }
    }

    /**
     * Update an existing student profile
     * @param userId User ID
     * @param request Profile update request
     * @return The updated profile
     */
    @PutMapping("/{userId}")
    public ResponseEntity<StudentProfileDTO> updateProfile(@PathVariable Long userId, @RequestBody CreateStudentProfileRequest request) {
        try {
            // Ensure the userId in the URL matches the one in the request
            if (request.getUserId() != null && !request.getUserId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID in request body does not match URL");
            }

            // Set the userId in the request
            request.setUserId(userId);

            // Create profile if it doesn't exist
            if (!studentProfileService.existsByUserId(userId)) {
                return createProfile(request);
            }

            StudentProfileDTO updatedProfile = studentProfileService.updateProfile(userId, request);
            return ResponseEntity.ok(updatedProfile);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating student profile: " + e.getMessage());
        }
    }

    /**
     * Delete a student profile
     * @param id Profile ID
     * @return No content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable Long id) {
        try {
            studentProfileService.deleteProfile(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting student profile: " + e.getMessage());
        }
    }
} 