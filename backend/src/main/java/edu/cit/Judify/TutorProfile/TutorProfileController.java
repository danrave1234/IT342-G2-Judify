package edu.cit.Judify.TutorProfile;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.cit.Judify.TutorProfile.DTO.TutorProfileDTO;
import edu.cit.Judify.TutorProfile.DTO.TutorRegistrationDTO;
import edu.cit.Judify.TutorSubject.DTO.TutorSubjectDTO;
import edu.cit.Judify.TutorSubject.TutorSubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/tutors")
@CrossOrigin(origins = "*")
@Tag(name = "Tutor Profile", description = "Tutor Profile management endpoints")
public class TutorProfileController {

    private final TutorProfileService tutorProfileService;
    private final TutorSubjectService tutorSubjectService;

    @Autowired
    public TutorProfileController(TutorProfileService tutorProfileService, TutorSubjectService tutorSubjectService) {
        this.tutorProfileService = tutorProfileService;
        this.tutorSubjectService = tutorSubjectService;
    }

    /**
     * Root endpoint for /api/tutors to match the mobile app's request
     * This is needed specifically for the map functionality on the mobile app
     */
    @Operation(summary = "Get all tutor profiles (mobile endpoint)", 
               description = "Returns all tutor profiles for the mobile app map")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved all tutor profiles",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = TutorProfileDTO.class)))
    })
    @GetMapping
    public ResponseEntity<List<TutorProfileDTO>> getAllTutorProfilesForMobile() {
        List<TutorProfileDTO> tutors = tutorProfileService.getAllTutorProfiles();
        return ResponseEntity.ok(tutors);
    }

    @Operation(summary = "Get all tutor profiles", description = "Returns a list of all tutor profiles")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved all tutor profiles",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = TutorProfileDTO.class)))
    })
    @GetMapping("/getAllProfiles")
    public ResponseEntity<List<TutorProfileDTO>> getAllTutorProfiles() {
        List<TutorProfileDTO> tutors = tutorProfileService.getAllTutorProfiles();
        return ResponseEntity.ok(tutors);
    }

    @Operation(summary = "Get paginated tutor profiles with filtering", 
               description = "Returns a paginated list of tutor profiles with optional filtering")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved filtered tutor profiles")
    })
    @GetMapping("/getAllProfilesPaginated")
    public ResponseEntity<Page<TutorProfileDTO>> getAllTutorProfilesPaginated(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by expertise (renamed to 'Course' in the frontend)") @RequestParam(required = false) String expertise,
            @Parameter(description = "Filter by minimum hourly rate (optional)") @RequestParam(required = false) Double minRate,
            @Parameter(description = "Filter by maximum hourly rate (optional)") @RequestParam(required = false) Double maxRate,
            @Parameter(description = "Filter by minimum rating (optional)") @RequestParam(required = false) Double minRating) {

        Page<TutorProfileDTO> tutors = tutorProfileService.getAllTutorProfilesPaginated(
                page, size, expertise, minRate, maxRate, minRating);
        return ResponseEntity.ok(tutors);
    }

    @Operation(summary = "Get tutor profile by ID", description = "Returns a tutor profile by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the tutor profile"),
        @ApiResponse(responseCode = "404", description = "Tutor profile not found")
    })
    @GetMapping("/findById/{tutorId}")
    public ResponseEntity<TutorProfileDTO> getTutorProfileById(
            @Parameter(description = "Tutor profile ID") @PathVariable Long tutorId) {
        try {
            TutorProfileDTO tutor = tutorProfileService.getTutorProfileById(tutorId);
            return ResponseEntity.ok(tutor);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get user ID by tutor ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found user ID"),
            @ApiResponse(responseCode = "404", description = "Tutor profile not found")
    })
    @GetMapping("/getUserId/{tutorId}")
    public ResponseEntity<Long> getUserIdByTutorId(
            @Parameter(description = "Tutor profile ID") @PathVariable Long tutorId) {
        try {
            TutorProfileDTO tutor = tutorProfileService.getTutorProfileById(tutorId);
            return ResponseEntity.ok(tutor.getUserId());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get tutor ID by user ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found tutor ID"),
            @ApiResponse(responseCode = "404", description = "Tutor profile not found for this user ID")
    })
    @GetMapping("/getTutorId/{userId}")
    public ResponseEntity<Long> getTutorIdByUserId(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        try {
            Optional<TutorProfileEntity> tutorProfileOpt = tutorProfileService.findByUserId(userId);
            if (tutorProfileOpt.isPresent()) {
                return ResponseEntity.ok(tutorProfileOpt.get().getId());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get tutor profile by user ID", description = "Returns a tutor profile associated with the given user ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the tutor profile"),
        @ApiResponse(responseCode = "404", description = "Tutor profile not found for the given user ID")
    })
    @GetMapping("/findByUserId/{userId}")
    public ResponseEntity<TutorProfileDTO> getTutorProfileByUserId(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        try {
            TutorProfileDTO tutor = tutorProfileService.getTutorProfileByUserId(userId);
            return ResponseEntity.ok(tutor);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Create a new tutor profile for user", description = "Creates a new tutor profile for a specific user ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Tutor profile successfully created"),
        @ApiResponse(responseCode = "400", description = "Invalid input or user not found")
    })
    @PostMapping("/createProfile/user/{userId}")
    public ResponseEntity<TutorProfileDTO> createTutorProfileForUser(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Tutor profile data", required = true)
            @RequestBody TutorProfileDTO tutorProfileDTO) {
        try {
            // Set the userId in the DTO
            tutorProfileDTO.setUserId(userId);

            TutorProfileDTO createdProfile = tutorProfileService.createTutorProfile(tutorProfileDTO);
            return new ResponseEntity<>(createdProfile, HttpStatus.CREATED);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Update a tutor profile", description = "Updates an existing tutor profile")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tutor profile successfully updated"),
        @ApiResponse(responseCode = "404", description = "Tutor profile not found")
    })
    @PutMapping("/updateProfile/{id}")
    public ResponseEntity<TutorProfileDTO> updateTutorProfile(
            @Parameter(description = "Tutor profile ID") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated tutor profile data", required = true)
            @RequestBody TutorProfileDTO tutorProfileDTO) {
        try {
            TutorProfileDTO updatedProfile = tutorProfileService.updateTutorProfile(id, tutorProfileDTO);
            return ResponseEntity.ok(updatedProfile);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Delete a tutor profile", description = "Deletes a tutor profile by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Tutor profile successfully deleted"),
        @ApiResponse(responseCode = "404", description = "Tutor profile not found")
    })
    @DeleteMapping("/deleteProfile/{id}")
    public ResponseEntity<Void> deleteTutorProfile(
            @Parameter(description = "Tutor profile ID") @PathVariable Long id) {
        try {
        tutorProfileService.deleteTutorProfile(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Search tutor profiles by subject", description = "Returns a list of tutor profiles that teach the specified subject")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved tutor profiles")
    })
    @GetMapping("/searchBySubject")
    public ResponseEntity<List<TutorProfileDTO>> searchTutorProfiles(
            @Parameter(description = "Subject to search for") @RequestParam String subject) {
        List<TutorProfileDTO> tutors = tutorProfileService.searchTutorProfiles(subject);
        return ResponseEntity.ok(tutors);
    }

    @Operation(summary = "Update tutor rating", description = "Updates the rating of a tutor profile")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tutor rating successfully updated"),
        @ApiResponse(responseCode = "404", description = "Tutor profile not found")
    })
    @PutMapping("/updateRating/{id}")
    public ResponseEntity<TutorProfileDTO> updateTutorRating(
            @Parameter(description = "Tutor profile ID") @PathVariable Long id,
            @Parameter(description = "New rating value") @RequestParam Double rating) {
        try {
            TutorProfileDTO updatedProfile = tutorProfileService.updateTutorRating(id, rating);
            return ResponseEntity.ok(updatedProfile);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Update tutor location", description = "Updates the location (latitude and longitude) of a tutor profile")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tutor location successfully updated"),
        @ApiResponse(responseCode = "404", description = "Tutor profile not found")
    })
    @PutMapping("/updateLocation/{id}")
    public ResponseEntity<TutorProfileDTO> updateTutorLocation(
            @Parameter(description = "Tutor profile ID") @PathVariable Long id,
            @Parameter(description = "Latitude coordinate") @RequestParam Double latitude,
            @Parameter(description = "Longitude coordinate") @RequestParam Double longitude) {
        try {
            TutorProfileDTO updatedProfile = tutorProfileService.updateTutorLocation(id, latitude, longitude);
            return ResponseEntity.ok(updatedProfile);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get subjects for a tutor profile", description = "Returns all subjects associated with a specific tutor profile")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved subjects"),
        @ApiResponse(responseCode = "404", description = "Tutor profile not found")
    })
    @GetMapping("/{tutorProfileId}/subjects")
    public ResponseEntity<List<TutorSubjectDTO>> getSubjectsForTutor(
            @Parameter(description = "Tutor profile ID") @PathVariable Long tutorProfileId) {
        try {
            // First check if the profile exists
            tutorProfileService.getTutorProfileById(tutorProfileId);

            List<TutorSubjectDTO> subjects = tutorSubjectService.getSubjectsByTutorProfileId(tutorProfileId);
            return ResponseEntity.ok(subjects);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Add subjects to a tutor profile", description = "Adds multiple subjects to a specific tutor profile")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Subjects successfully added"),
        @ApiResponse(responseCode = "404", description = "Tutor profile not found")
    })
    @PostMapping("/{tutorProfileId}/subjects")
    public ResponseEntity<List<TutorSubjectDTO>> addSubjectsToTutor(
            @Parameter(description = "Tutor profile ID") @PathVariable Long tutorProfileId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "List of subjects", required = true)
            @RequestBody List<String> subjects) {
        try {
            // First check if the profile exists
            tutorProfileService.getTutorProfileById(tutorProfileId);

            List<TutorSubjectDTO> addedSubjects = tutorSubjectService.addSubjectsForTutor(tutorProfileId, subjects);
            return new ResponseEntity<>(addedSubjects, HttpStatus.CREATED);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Delete all subjects for a tutor profile", description = "Deletes all subjects associated with a specific tutor profile")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Subjects successfully deleted"),
        @ApiResponse(responseCode = "404", description = "Tutor profile not found")
    })
    @DeleteMapping("/{tutorProfileId}/subjects")
    public ResponseEntity<Void> deleteAllSubjectsForTutor(
            @Parameter(description = "Tutor profile ID") @PathVariable Long tutorProfileId) {
        try {
            // First check if the profile exists
            tutorProfileService.getTutorProfileById(tutorProfileId);

            tutorSubjectService.deleteAllSubjectsForTutor(tutorProfileId);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get random tutor profiles", description = "Returns a list of random tutor profiles with a limit to avoid performance issues")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved random tutor profiles")
    })
    @GetMapping("/random")
    public ResponseEntity<List<TutorProfileDTO>> getRandomTutorProfiles(
            @Parameter(description = "Maximum number of tutor profiles to return (default and max is 10)") 
            @RequestParam(defaultValue = "10") int limit) {
        List<TutorProfileDTO> randomTutors = tutorProfileService.getRandomTutorProfiles(limit);
        return ResponseEntity.ok(randomTutors);
    }

    @Operation(summary = "Register a new tutor", description = "Creates a new user with TUTOR role and associated tutor profile")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Tutor successfully registered",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = TutorProfileDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping("/register")
    public ResponseEntity<TutorProfileDTO> registerTutor(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Tutor registration data", required = true)
            @RequestBody TutorRegistrationDTO registrationDTO) {
        try {
            TutorProfileDTO createdProfile = tutorProfileService.registerTutor(registrationDTO);
            return new ResponseEntity<>(createdProfile, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

