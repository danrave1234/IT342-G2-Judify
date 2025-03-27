package edu.cit.Judify.TutorSubject;

import edu.cit.Judify.TutorSubject.DTO.TutorSubjectDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@CrossOrigin(origins = "*")
@Tag(name = "Tutor Subjects", description = "Tutor subject management endpoints")
public class TutorSubjectController {

    private final TutorSubjectService tutorSubjectService;

    @Autowired
    public TutorSubjectController(TutorSubjectService tutorSubjectService) {
        this.tutorSubjectService = tutorSubjectService;
    }

    @Operation(summary = "Get subjects by tutor profile ID", description = "Returns all subjects for a specific tutor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved subjects")
    })
    @GetMapping("/by-tutor/{tutorProfileId}")
    public ResponseEntity<List<TutorSubjectDTO>> getSubjectsByTutorProfileId(
            @Parameter(description = "Tutor profile ID") @PathVariable Long tutorProfileId) {
        List<TutorSubjectDTO> subjects = tutorSubjectService.getSubjectsByTutorProfileId(tutorProfileId);
        return ResponseEntity.ok(subjects);
    }

    @Operation(summary = "Search subjects", description = "Searches for tutor subjects containing the specified query")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved subjects")
    })
    @GetMapping("/search")
    public ResponseEntity<List<TutorSubjectDTO>> searchSubjects(
            @Parameter(description = "Search query") @RequestParam String query) {
        List<TutorSubjectDTO> subjects = tutorSubjectService.searchSubjects(query);
        return ResponseEntity.ok(subjects);
    }

    @Operation(summary = "Add a subject", description = "Adds a new subject for a tutor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Subject successfully added",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = TutorSubjectDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input or tutor profile not found")
    })
    @PostMapping("/add")
    public ResponseEntity<TutorSubjectDTO> addSubject(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Subject data", required = true)
            @RequestBody TutorSubjectDTO subjectDTO) {
        try {
            TutorSubjectDTO createdSubject = tutorSubjectService.addSubject(subjectDTO);
            return new ResponseEntity<>(createdSubject, HttpStatus.CREATED);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Update a subject", description = "Updates an existing subject")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Subject successfully updated"),
        @ApiResponse(responseCode = "404", description = "Subject not found")
    })
    @PutMapping("/update/{id}")
    public ResponseEntity<TutorSubjectDTO> updateSubject(
            @Parameter(description = "Subject ID") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated subject data", required = true)
            @RequestBody TutorSubjectDTO subjectDTO) {
        try {
            TutorSubjectDTO updatedSubject = tutorSubjectService.updateSubject(id, subjectDTO);
            return ResponseEntity.ok(updatedSubject);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Delete a subject", description = "Deletes a subject by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Subject successfully deleted"),
        @ApiResponse(responseCode = "404", description = "Subject not found")
    })
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteSubject(
            @Parameter(description = "Subject ID") @PathVariable Long id) {
        try {
            tutorSubjectService.deleteSubject(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Add multiple subjects for a tutor", description = "Adds multiple subjects for a specific tutor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Subjects successfully added"),
        @ApiResponse(responseCode = "400", description = "Invalid input or tutor profile not found")
    })
    @PostMapping("/add-multiple/{tutorProfileId}")
    public ResponseEntity<List<TutorSubjectDTO>> addSubjectsForTutor(
            @Parameter(description = "Tutor profile ID") @PathVariable Long tutorProfileId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "List of subjects", required = true)
            @RequestBody List<String> subjects) {
        try {
            List<TutorSubjectDTO> createdSubjects = tutorSubjectService.addSubjectsForTutor(tutorProfileId, subjects);
            return new ResponseEntity<>(createdSubjects, HttpStatus.CREATED);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Delete all subjects for a tutor", description = "Deletes all subjects associated with a specific tutor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Subjects successfully deleted"),
        @ApiResponse(responseCode = "404", description = "Tutor profile not found")
    })
    @DeleteMapping("/delete-all/{tutorProfileId}")
    public ResponseEntity<Void> deleteAllSubjectsForTutor(
            @Parameter(description = "Tutor profile ID") @PathVariable Long tutorProfileId) {
        try {
            tutorSubjectService.deleteAllSubjectsForTutor(tutorProfileId);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
} 