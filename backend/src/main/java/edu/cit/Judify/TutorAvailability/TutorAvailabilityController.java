package edu.cit.Judify.TutorAvailability;

import edu.cit.Judify.TutorAvailability.DTO.TutorAvailabilityDTO;
import edu.cit.Judify.TutorAvailability.DTO.TutorAvailabilityDTOMapper;
import edu.cit.Judify.User.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tutor-availability")
@CrossOrigin(origins = "*")
@Tag(name = "Tutor Availability", description = "Tutor availability management endpoints")
public class TutorAvailabilityController {

    private final TutorAvailabilityService availabilityService;
    private final TutorAvailabilityDTOMapper availabilityDTOMapper;

    @Autowired
    public TutorAvailabilityController(TutorAvailabilityService availabilityService, 
                                      TutorAvailabilityDTOMapper availabilityDTOMapper) {
        this.availabilityService = availabilityService;
        this.availabilityDTOMapper = availabilityDTOMapper;
    }

    @Operation(summary = "Create a new availability slot", description = "Creates a new availability time slot for a tutor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Availability slot successfully created",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = TutorAvailabilityDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping("/createAvailability")
    public ResponseEntity<TutorAvailabilityDTO> createAvailability(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Availability slot data to create", required = true)
            @RequestBody TutorAvailabilityDTO availabilityDTO) {
        TutorAvailabilityEntity availability = availabilityDTOMapper.toEntity(availabilityDTO); // Tutor will be set by service
        return ResponseEntity.ok(availabilityDTOMapper.toDTO(availabilityService.createAvailability(availability)));
    }

    @Operation(summary = "Get availability slot by ID", description = "Returns an availability slot by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the availability slot"),
        @ApiResponse(responseCode = "404", description = "Availability slot not found")
    })
    @GetMapping("/findById/{id}")
    public ResponseEntity<TutorAvailabilityDTO> getAvailabilityById(
            @Parameter(description = "Availability slot ID") @PathVariable Long id) {
        return availabilityService.getAvailabilityById(id)
                .map(availabilityDTOMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get tutor's availability slots", description = "Returns all availability slots for a specific tutor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved tutor's availability slots")
    })
    @GetMapping("/findByTutor/{tutorId}")
    public ResponseEntity<List<TutorAvailabilityDTO>> getTutorAvailability(
            @Parameter(description = "Tutor ID") @PathVariable UserEntity tutor) {
        return ResponseEntity.ok(availabilityService.getTutorAvailability(tutor)
                .stream()
                .map(availabilityDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get availability slots by day", description = "Returns all availability slots for a specific day of the week")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved availability slots by day")
    })
    @GetMapping("/findByDay/{dayOfWeek}")
    public ResponseEntity<List<TutorAvailabilityDTO>> getAvailabilityByDay(
            @Parameter(description = "Day of week (e.g., MONDAY, TUESDAY)") @PathVariable String dayOfWeek) {
        return ResponseEntity.ok(availabilityService.getAvailabilityByDay(dayOfWeek)
                .stream()
                .map(availabilityDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get tutor's availability by day", description = "Returns all availability slots for a specific tutor on a specific day")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved tutor's availability for the specified day")
    })
    @GetMapping("/findByTutorAndDay/{tutorId}/{dayOfWeek}")
    public ResponseEntity<List<TutorAvailabilityDTO>> getTutorAvailabilityByDay(
            @Parameter(description = "Tutor ID") @PathVariable UserEntity tutor,
            @Parameter(description = "Day of week (e.g., MONDAY, TUESDAY)") @PathVariable String dayOfWeek) {
        return ResponseEntity.ok(availabilityService.getTutorAvailabilityByDay(tutor, dayOfWeek)
                .stream()
                .map(availabilityDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Update an availability slot", description = "Updates an existing availability slot")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Availability slot successfully updated"),
        @ApiResponse(responseCode = "404", description = "Availability slot not found")
    })
    @PutMapping("/updateAvailability/{id}")
    public ResponseEntity<TutorAvailabilityDTO> updateAvailability(
            @Parameter(description = "Availability slot ID") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated availability slot data", required = true)
            @RequestBody TutorAvailabilityDTO availabilityDTO) {
        TutorAvailabilityEntity availability = availabilityDTOMapper.toEntity(availabilityDTO);
        return ResponseEntity.ok(availabilityDTOMapper.toDTO(
                availabilityService.updateAvailability(id, availability)));
    }

    @Operation(summary = "Delete an availability slot", description = "Deletes an availability slot by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Availability slot successfully deleted"),
        @ApiResponse(responseCode = "404", description = "Availability slot not found")
    })
    @DeleteMapping("/deleteAvailability/{id}")
    public ResponseEntity<Void> deleteAvailability(
            @Parameter(description = "Availability slot ID") @PathVariable Long id) {
        availabilityService.deleteAvailability(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete all availability slots for a tutor", description = "Deletes all availability slots for a specific tutor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All tutor's availability slots successfully deleted")
    })
    @DeleteMapping("/deleteAllForTutor/{tutorId}")
    public ResponseEntity<Void> deleteTutorAvailability(
            @Parameter(description = "Tutor ID") @PathVariable UserEntity tutor) {
        availabilityService.deleteTutorAvailability(tutor);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Check time slot availability", description = "Checks if a specific time slot is available for a tutor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully checked availability")
    })
    @GetMapping("/checkAvailability")
    public ResponseEntity<Boolean> isTimeSlotAvailable(
            @Parameter(description = "Tutor ID") @RequestParam UserEntity tutor,
            @Parameter(description = "Day of week (e.g., MONDAY, TUESDAY)") @RequestParam String dayOfWeek,
            @Parameter(description = "Start time (HH:MM)") @RequestParam String startTime,
            @Parameter(description = "End time (HH:MM)") @RequestParam String endTime) {
        return ResponseEntity.ok(availabilityService.isTimeSlotAvailable(tutor, dayOfWeek, startTime, endTime));
    }
} 