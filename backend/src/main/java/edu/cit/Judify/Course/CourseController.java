package edu.cit.Judify.Course;

import edu.cit.Judify.Course.DTO.CourseDTO;
import edu.cit.Judify.TutorProfile.DTO.TutorProfileDTO;
import edu.cit.Judify.TutorProfile.TutorProfileService;
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
@RequestMapping("/api/courses")
@Tag(name = "Course", description = "Course management APIs")
public class CourseController {

    private final CourseService courseService;
    private final TutorProfileService tutorProfileService;

    @Autowired
    public CourseController(CourseService courseService, TutorProfileService tutorProfileService) {
        this.courseService = courseService;
        this.tutorProfileService = tutorProfileService;
    }

    @GetMapping
    @Operation(summary = "Get all courses", description = "Retrieve a list of all courses")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Courses retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CourseDTO.class)))
    })
    public ResponseEntity<List<CourseDTO>> getAllCourses() {
        List<CourseDTO> courses = courseService.getAllCourses();
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get course by ID", description = "Retrieve a course by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CourseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    public ResponseEntity<CourseDTO> getCourseById(
            @Parameter(description = "Course ID", required = true) @PathVariable Long id) {
        try {
            CourseDTO course = courseService.getCourseById(id);
            return ResponseEntity.ok(course);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/tutor/{tutorId}")
    @Operation(summary = "Get courses by tutor ID", description = "Retrieve all courses created by a specific tutor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Courses retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CourseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Tutor not found")
    })
    public ResponseEntity<List<CourseDTO>> getCoursesByTutorId(
            @Parameter(description = "Tutor ID", required = true) @PathVariable Long tutorId) {
        try {
            List<CourseDTO> courses = courseService.getCoursesByTutorId(tutorId);
            return ResponseEntity.ok(courses);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get courses by category", description = "Retrieve all courses in a specific category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Courses retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CourseDTO.class)))
    })
    public ResponseEntity<List<CourseDTO>> getCoursesByCategory(
            @Parameter(description = "Course category", required = true) @PathVariable String category) {
        List<CourseDTO> courses = courseService.getCoursesByCategory(category);
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/search")
    @Operation(summary = "Search courses by title", description = "Search for courses with titles containing the search term")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Courses retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CourseDTO.class)))
    })
    public ResponseEntity<List<CourseDTO>> searchCoursesByTitle(
            @Parameter(description = "Search term", required = true) @RequestParam String title) {
        List<CourseDTO> courses = courseService.searchCoursesByTitle(title);
        return ResponseEntity.ok(courses);
    }

    @PostMapping("/tutor/{tutorId}")
    @Operation(summary = "Create a new course", description = "Create a new course for a specific tutor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Course created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CourseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Tutor not found")
    })
    public ResponseEntity<CourseDTO> createCourse(
            @Parameter(description = "Tutor ID", required = true) @PathVariable Long tutorId,
            @Parameter(description = "Course details", required = true) @RequestBody CourseDTO courseDTO) {
        try {
            CourseDTO createdCourse = courseService.createCourse(courseDTO, tutorId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCourse);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a course", description = "Update an existing course")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CourseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    public ResponseEntity<CourseDTO> updateCourse(
            @Parameter(description = "Course ID", required = true) @PathVariable Long id,
            @Parameter(description = "Updated course details", required = true) @RequestBody CourseDTO courseDTO) {
        try {
            CourseDTO updatedCourse = courseService.updateCourse(id, courseDTO);
            return ResponseEntity.ok(updatedCourse);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a course", description = "Delete an existing course")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Course deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    public ResponseEntity<Void> deleteCourse(
            @Parameter(description = "Course ID", required = true) @PathVariable Long id) {
        try {
            courseService.deleteCourse(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/tutor")
    @Operation(summary = "Get tutor profile by course ID", description = "Retrieve the tutor profile associated with a specific course")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tutor profile retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TutorProfileDTO.class))),
            @ApiResponse(responseCode = "404", description = "Course or tutor not found")
    })
    public ResponseEntity<TutorProfileDTO> getTutorProfileByCourseId(
            @Parameter(description = "Course ID", required = true) @PathVariable Long id) {
        try {
            // Get the course by ID
            CourseDTO course = courseService.getCourseById(id);

            // Get the tutor profile by tutor ID
            TutorProfileDTO tutorProfile = tutorProfileService.getTutorProfileById(course.getTutorId());

            return ResponseEntity.ok(tutorProfile);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
