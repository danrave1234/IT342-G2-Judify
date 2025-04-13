package edu.cit.Judify.Course;

import edu.cit.Judify.Course.DTO.CourseDTO;
import edu.cit.Judify.TutorProfile.TutorProfileEntity;
import edu.cit.Judify.TutorProfile.TutorProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final TutorProfileRepository tutorProfileRepository;

    @Autowired
    public CourseService(CourseRepository courseRepository, TutorProfileRepository tutorProfileRepository) {
        this.courseRepository = courseRepository;
        this.tutorProfileRepository = tutorProfileRepository;
    }

    /**
     * Get all courses
     *
     * @return List of course DTOs
     */
    public List<CourseDTO> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get course by ID
     *
     * @param id Course ID
     * @return Course DTO
     * @throws EntityNotFoundException if course not found
     */
    public CourseDTO getCourseById(Long id) {
        CourseEntity course = courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + id));
        return convertToDTO(course);
    }

    /**
     * Get courses by tutor ID
     *
     * @param tutorId Tutor ID
     * @return List of course DTOs
     * @throws EntityNotFoundException if tutor not found
     */
    public List<CourseDTO> getCoursesByTutorId(Long tutorId) {
        TutorProfileEntity tutor = tutorProfileRepository.findById(tutorId)
                .orElseThrow(() -> new EntityNotFoundException("Tutor not found with id: " + tutorId));
        
        return courseRepository.findByTutor(tutor).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get courses by category
     *
     * @param category Course category
     * @return List of course DTOs
     */
    public List<CourseDTO> getCoursesByCategory(String category) {
        return courseRepository.findByCategory(category).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Search courses by title
     *
     * @param title Search term
     * @return List of course DTOs
     */
    public List<CourseDTO> searchCoursesByTitle(String title) {
        return courseRepository.findByTitleContainingIgnoreCase(title).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Create a new course
     *
     * @param courseDTO Course DTO
     * @param tutorId Tutor ID
     * @return Created course DTO
     * @throws EntityNotFoundException if tutor not found
     */
    public CourseDTO createCourse(CourseDTO courseDTO, Long tutorId) {
        TutorProfileEntity tutor = tutorProfileRepository.findById(tutorId)
                .orElseThrow(() -> new EntityNotFoundException("Tutor not found with id: " + tutorId));
        
        CourseEntity course = new CourseEntity();
        course.setTitle(courseDTO.getTitle());
        course.setSubtitle(courseDTO.getSubtitle());
        course.setDescription(courseDTO.getDescription());
        course.setTutor(tutor);
        course.setCategory(courseDTO.getCategory());
        course.setPrice(courseDTO.getPrice());
        
        CourseEntity savedCourse = courseRepository.save(course);
        return convertToDTO(savedCourse);
    }

    /**
     * Update an existing course
     *
     * @param id Course ID
     * @param courseDTO Course DTO
     * @return Updated course DTO
     * @throws EntityNotFoundException if course not found
     */
    public CourseDTO updateCourse(Long id, CourseDTO courseDTO) {
        CourseEntity course = courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + id));
        
        course.setTitle(courseDTO.getTitle());
        course.setSubtitle(courseDTO.getSubtitle());
        course.setDescription(courseDTO.getDescription());
        course.setCategory(courseDTO.getCategory());
        course.setPrice(courseDTO.getPrice());
        
        CourseEntity updatedCourse = courseRepository.save(course);
        return convertToDTO(updatedCourse);
    }

    /**
     * Delete a course
     *
     * @param id Course ID
     * @throws EntityNotFoundException if course not found
     */
    public void deleteCourse(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new EntityNotFoundException("Course not found with id: " + id);
        }
        courseRepository.deleteById(id);
    }

    /**
     * Convert CourseEntity to CourseDTO
     *
     * @param course Course entity
     * @return Course DTO
     */
    private CourseDTO convertToDTO(CourseEntity course) {
        CourseDTO dto = new CourseDTO();
        dto.setId(course.getId());
        dto.setTitle(course.getTitle());
        dto.setSubtitle(course.getSubtitle());
        dto.setDescription(course.getDescription());
        dto.setTutorId(course.getTutor().getId());
        dto.setTutorName(course.getTutor().getUser().getFirstName() + " " + course.getTutor().getUser().getLastName());
        dto.setCategory(course.getCategory());
        dto.setPrice(course.getPrice());
        dto.setCreatedAt(course.getCreatedAt());
        return dto;
    }
}