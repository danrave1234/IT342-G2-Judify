package edu.cit.Judify.TutorProfile;

import edu.cit.Judify.TutorProfile.DTO.TutorProfileDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tutors")
@CrossOrigin(origins = "*")
public class TutorProfileController {

    private final TutorProfileService tutorProfileService;

    @Autowired
    public TutorProfileController(TutorProfileService tutorProfileService) {
        this.tutorProfileService = tutorProfileService;
    }

    @GetMapping("/getAllProfiles")
    public ResponseEntity<List<TutorProfileDTO>> getAllTutorProfiles() {
        List<TutorProfileDTO> tutors = tutorProfileService.getAllTutorProfiles();
        return ResponseEntity.ok(tutors);
    }

    @GetMapping("/getAllProfilesPaginated")
    public ResponseEntity<Page<TutorProfileDTO>> getAllTutorProfilesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String expertise,
            @RequestParam(required = false) Double minRate,
            @RequestParam(required = false) Double maxRate,
            @RequestParam(required = false) Double minRating) {
        
        Page<TutorProfileDTO> tutors = tutorProfileService.getAllTutorProfilesPaginated(
                page, size, expertise, minRate, maxRate, minRating);
        return ResponseEntity.ok(tutors);
    }

    @GetMapping("/findById/{id}")
    public ResponseEntity<TutorProfileDTO> getTutorProfileById(@PathVariable Long id) {
        try {
            TutorProfileDTO tutor = tutorProfileService.getTutorProfileById(id);
            return ResponseEntity.ok(tutor);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/findByUserId/{userId}")
    public ResponseEntity<TutorProfileDTO> getTutorProfileByUserId(@PathVariable Long userId) {
        try {
            TutorProfileDTO tutor = tutorProfileService.getTutorProfileByUserId(userId);
            return ResponseEntity.ok(tutor);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/createProfile")
    public ResponseEntity<TutorProfileDTO> createTutorProfile(@RequestBody TutorProfileDTO tutorProfileDTO) {
        try {
            TutorProfileDTO createdProfile = tutorProfileService.createTutorProfile(tutorProfileDTO);
            return new ResponseEntity<>(createdProfile, HttpStatus.CREATED);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/updateProfile/{id}")
    public ResponseEntity<TutorProfileDTO> updateTutorProfile(
            @PathVariable Long id,
            @RequestBody TutorProfileDTO tutorProfileDTO) {
        try {
            TutorProfileDTO updatedProfile = tutorProfileService.updateTutorProfile(id, tutorProfileDTO);
            return ResponseEntity.ok(updatedProfile);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/deleteProfile/{id}")
    public ResponseEntity<Void> deleteTutorProfile(@PathVariable Long id) {
        try {
            tutorProfileService.deleteTutorProfile(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/searchBySubject")
    public ResponseEntity<List<TutorProfileDTO>> searchTutorProfiles(
            @RequestParam String subject) {
        List<TutorProfileDTO> tutors = tutorProfileService.searchTutorProfiles(subject);
        return ResponseEntity.ok(tutors);
    }

    @PutMapping("/updateRating/{id}")
    public ResponseEntity<TutorProfileDTO> updateTutorRating(
            @PathVariable Long id,
            @RequestParam Double rating) {
        try {
            TutorProfileDTO updatedProfile = tutorProfileService.updateTutorRating(id, rating);
            return ResponseEntity.ok(updatedProfile);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
} 