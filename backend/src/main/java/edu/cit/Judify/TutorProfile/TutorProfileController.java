package edu.cit.Judify.TutorProfile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tutor-profiles")
@CrossOrigin(origins = "*")
public class TutorProfileController {

    private final TutorProfileService tutorProfileService;

    @Autowired
    public TutorProfileController(TutorProfileService tutorProfileService) {
        this.tutorProfileService = tutorProfileService;
    }

    @PostMapping
    public ResponseEntity<TutorProfileEntity> createTutorProfile(@RequestBody TutorProfileEntity profile) {
        return ResponseEntity.ok(tutorProfileService.createTutorProfile(profile));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TutorProfileEntity> getTutorProfileById(@PathVariable Long id) {
        return tutorProfileService.getTutorProfileById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/expertise/{expertise}")
    public ResponseEntity<List<TutorProfileEntity>> findTutorsByExpertise(@PathVariable String expertise) {
        return ResponseEntity.ok(tutorProfileService.findTutorsByExpertise(expertise));
    }

    @GetMapping("/price-range")
    public ResponseEntity<List<TutorProfileEntity>> findTutorsByPriceRange(
            @RequestParam Double minRate,
            @RequestParam Double maxRate) {
        return ResponseEntity.ok(tutorProfileService.findTutorsByPriceRange(minRate, maxRate));
    }

    @GetMapping("/top-rated")
    public ResponseEntity<List<TutorProfileEntity>> findTopRatedTutors(
            @RequestParam(defaultValue = "4.0") Double minRating) {
        return ResponseEntity.ok(tutorProfileService.findTopRatedTutors(minRating));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TutorProfileEntity> updateTutorProfile(
            @PathVariable Long id,
            @RequestBody TutorProfileEntity profileDetails) {
        return ResponseEntity.ok(tutorProfileService.updateTutorProfile(id, profileDetails));
    }

    @PutMapping("/{id}/rating")
    public ResponseEntity<TutorProfileEntity> updateTutorRating(
            @PathVariable Long id,
            @RequestParam Double newRating,
            @RequestParam Integer newCount) {
        return ResponseEntity.ok(tutorProfileService.updateTutorRating(id, newRating, newCount));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTutorProfile(@PathVariable Long id) {
        tutorProfileService.deleteTutorProfile(id);
        return ResponseEntity.ok().build();
    }
} 