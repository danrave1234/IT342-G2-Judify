package edu.cit.Judify.TutorAvailability;

import edu.cit.Judify.TutorAvailability.DTO.TutorAvailabilityDTO;
import edu.cit.Judify.TutorAvailability.DTO.TutorAvailabilityDTOMapper;
import edu.cit.Judify.User.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tutor-availability")
@CrossOrigin(origins = "*")
public class TutorAvailabilityController {

    private final TutorAvailabilityService availabilityService;
    private final TutorAvailabilityDTOMapper availabilityDTOMapper;

    @Autowired
    public TutorAvailabilityController(TutorAvailabilityService availabilityService, 
                                      TutorAvailabilityDTOMapper availabilityDTOMapper) {
        this.availabilityService = availabilityService;
        this.availabilityDTOMapper = availabilityDTOMapper;
    }

    @PostMapping("/createAvailability")
    public ResponseEntity<TutorAvailabilityDTO> createAvailability(
            @RequestBody TutorAvailabilityDTO availabilityDTO) {
        TutorAvailabilityEntity availability = availabilityDTOMapper.toEntity(availabilityDTO); // Tutor will be set by service
        return ResponseEntity.ok(availabilityDTOMapper.toDTO(availabilityService.createAvailability(availability)));
    }

    @GetMapping("/findById/{id}")
    public ResponseEntity<TutorAvailabilityDTO> getAvailabilityById(@PathVariable Long id) {
        return availabilityService.getAvailabilityById(id)
                .map(availabilityDTOMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/findByTutor/{tutorId}")
    public ResponseEntity<List<TutorAvailabilityDTO>> getTutorAvailability(
            @PathVariable UserEntity tutor) {
        return ResponseEntity.ok(availabilityService.getTutorAvailability(tutor)
                .stream()
                .map(availabilityDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/findByDay/{dayOfWeek}")
    public ResponseEntity<List<TutorAvailabilityDTO>> getAvailabilityByDay(
            @PathVariable String dayOfWeek) {
        return ResponseEntity.ok(availabilityService.getAvailabilityByDay(dayOfWeek)
                .stream()
                .map(availabilityDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/findByTutorAndDay/{tutorId}/{dayOfWeek}")
    public ResponseEntity<List<TutorAvailabilityDTO>> getTutorAvailabilityByDay(
            @PathVariable UserEntity tutor,
            @PathVariable String dayOfWeek) {
        return ResponseEntity.ok(availabilityService.getTutorAvailabilityByDay(tutor, dayOfWeek)
                .stream()
                .map(availabilityDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @PutMapping("/updateAvailability/{id}")
    public ResponseEntity<TutorAvailabilityDTO> updateAvailability(
            @PathVariable Long id,
            @RequestBody TutorAvailabilityDTO availabilityDTO) {
        TutorAvailabilityEntity availability = availabilityDTOMapper.toEntity(availabilityDTO);
        return ResponseEntity.ok(availabilityDTOMapper.toDTO(
                availabilityService.updateAvailability(id, availability)));
    }

    @DeleteMapping("/deleteAvailability/{id}")
    public ResponseEntity<Void> deleteAvailability(@PathVariable Long id) {
        availabilityService.deleteAvailability(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/deleteAllForTutor/{tutorId}")
    public ResponseEntity<Void> deleteTutorAvailability(@PathVariable UserEntity tutor) {
        availabilityService.deleteTutorAvailability(tutor);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/checkAvailability")
    public ResponseEntity<Boolean> isTimeSlotAvailable(
            @RequestParam UserEntity tutor,
            @RequestParam String dayOfWeek,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        return ResponseEntity.ok(availabilityService.isTimeSlotAvailable(tutor, dayOfWeek, startTime, endTime));
    }
} 