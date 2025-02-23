package edu.cit.Judify.TutorAvailability;

import edu.cit.Judify.User.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tutor-availability")
@CrossOrigin(origins = "*")
public class TutorAvailabilityController {

    private final TutorAvailabilityService availabilityService;

    @Autowired
    public TutorAvailabilityController(TutorAvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @PostMapping
    public ResponseEntity<TutorAvailabilityEntity> createAvailability(
            @RequestBody TutorAvailabilityEntity availability) {
        return ResponseEntity.ok(availabilityService.createAvailability(availability));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TutorAvailabilityEntity> getAvailabilityById(@PathVariable Long id) {
        return availabilityService.getAvailabilityById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tutor/{tutorId}")
    public ResponseEntity<List<TutorAvailabilityEntity>> getTutorAvailability(
            @PathVariable UserEntity tutor) {
        return ResponseEntity.ok(availabilityService.getTutorAvailability(tutor));
    }

    @GetMapping("/day/{dayOfWeek}")
    public ResponseEntity<List<TutorAvailabilityEntity>> getAvailabilityByDay(
            @PathVariable String dayOfWeek) {
        return ResponseEntity.ok(availabilityService.getAvailabilityByDay(dayOfWeek));
    }

    @GetMapping("/tutor/{tutorId}/day/{dayOfWeek}")
    public ResponseEntity<List<TutorAvailabilityEntity>> getTutorAvailabilityByDay(
            @PathVariable UserEntity tutor,
            @PathVariable String dayOfWeek) {
        return ResponseEntity.ok(availabilityService.getTutorAvailabilityByDay(tutor, dayOfWeek));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TutorAvailabilityEntity> updateAvailability(
            @PathVariable Long id,
            @RequestBody TutorAvailabilityEntity availabilityDetails) {
        return ResponseEntity.ok(availabilityService.updateAvailability(id, availabilityDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAvailability(@PathVariable Long id) {
        availabilityService.deleteAvailability(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/tutor/{tutorId}")
    public ResponseEntity<Void> deleteTutorAvailability(@PathVariable UserEntity tutor) {
        availabilityService.deleteTutorAvailability(tutor);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check-availability")
    public ResponseEntity<Boolean> isTimeSlotAvailable(
            @RequestParam UserEntity tutor,
            @RequestParam String dayOfWeek,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        return ResponseEntity.ok(availabilityService.isTimeSlotAvailable(tutor, dayOfWeek, startTime, endTime));
    }
} 