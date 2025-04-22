package edu.cit.Judify.Course;

import edu.cit.Judify.TutorProfile.TutorProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<CourseEntity, Long> {
    
    /**
     * Find all courses by tutor
     * 
     * @param tutor The tutor profile entity
     * @return List of courses created by the tutor
     */
    List<CourseEntity> findByTutor(TutorProfileEntity tutor);
    
    /**
     * Find all courses by category
     * 
     * @param category The course category
     * @return List of courses in the specified category
     */
    List<CourseEntity> findByCategory(String category);
    
    /**
     * Find all courses by title containing the search term (case-insensitive)
     * 
     * @param title The search term
     * @return List of courses with titles containing the search term
     */
    List<CourseEntity> findByTitleContainingIgnoreCase(String title);
    
    /**
     * Find all courses by tutor and category
     * 
     * @param tutor The tutor profile entity
     * @param category The course category
     * @return List of courses created by the tutor in the specified category
     */
    List<CourseEntity> findByTutorAndCategory(TutorProfileEntity tutor, String category);
}