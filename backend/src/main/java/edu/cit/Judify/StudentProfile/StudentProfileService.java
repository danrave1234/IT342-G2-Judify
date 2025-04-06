package edu.cit.Judify.StudentProfile;

import edu.cit.Judify.StudentProfile.DTO.CreateStudentProfileRequest;
import edu.cit.Judify.StudentProfile.DTO.LocationDTO;
import edu.cit.Judify.StudentProfile.DTO.StudentProfileDTO;
import edu.cit.Judify.User.UserEntity;
import edu.cit.Judify.User.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;

    @Autowired
    public StudentProfileService(StudentProfileRepository studentProfileRepository, UserRepository userRepository) {
        this.studentProfileRepository = studentProfileRepository;
        this.userRepository = userRepository;
    }

    /**
     * Find a student profile by ID
     * @param id Profile ID
     * @return The profile DTO
     * @throws EntityNotFoundException if profile not found
     */
    public StudentProfileDTO findById(Long id) {
        StudentProfileEntity entity = studentProfileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found with ID: " + id));
        return entityToDTO(entity);
    }

    /**
     * Find a student profile by user ID
     * @param userId User ID
     * @return The profile DTO
     * @throws EntityNotFoundException if profile not found
     */
    public StudentProfileDTO findByUserId(Long userId) {
        StudentProfileEntity entity = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found for user ID: " + userId));
        return entityToDTO(entity);
    }

    /**
     * Check if a profile exists for a user
     * @param userId User ID
     * @return True if exists, false otherwise
     */
    public boolean existsByUserId(Long userId) {
        return studentProfileRepository.existsByUserId(userId);
    }

    /**
     * Create a new student profile
     * @param request Profile creation request
     * @return The created profile DTO
     * @throws EntityNotFoundException if user not found
     */
    @Transactional
    public StudentProfileDTO createProfile(CreateStudentProfileRequest request) {
        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + request.getUserId()));

        // Check if profile already exists
        Optional<StudentProfileEntity> existingProfile = studentProfileRepository.findByUserId(request.getUserId());
        if (existingProfile.isPresent()) {
            throw new IllegalStateException("Profile already exists for user ID: " + request.getUserId());
        }

        StudentProfileEntity entity = new StudentProfileEntity();
        entity.setUser(user);
        updateEntityFromRequest(entity, request);

        StudentProfileEntity savedEntity = studentProfileRepository.save(entity);
        return entityToDTO(savedEntity);
    }

    /**
     * Update an existing student profile
     * @param userId User ID
     * @param request Profile update request
     * @return The updated profile DTO
     * @throws EntityNotFoundException if profile not found
     */
    @Transactional
    public StudentProfileDTO updateProfile(Long userId, CreateStudentProfileRequest request) {
        StudentProfileEntity entity = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found for user ID: " + userId));

        updateEntityFromRequest(entity, request);
        StudentProfileEntity savedEntity = studentProfileRepository.save(entity);
        return entityToDTO(savedEntity);
    }

    /**
     * Delete a student profile
     * @param id Profile ID
     * @throws EntityNotFoundException if profile not found
     */
    @Transactional
    public void deleteProfile(Long id) {
        if (!studentProfileRepository.existsById(id)) {
            throw new EntityNotFoundException("Student profile not found with ID: " + id);
        }
        studentProfileRepository.deleteById(id);
    }

    /**
     * Update entity fields from request
     * @param entity Entity to update
     * @param request Update request
     */
    private void updateEntityFromRequest(StudentProfileEntity entity, CreateStudentProfileRequest request) {
        entity.setBio(request.getBio());
        entity.setGradeLevel(request.getGradeLevel());
        entity.setSchool(request.getSchool());

        // Set location fields
        if (request.getLocation() != null) {
            entity.setCity(request.getLocation().getCity());
            entity.setState(request.getLocation().getState());
            entity.setCountry(request.getLocation().getCountry());
            entity.setLatitude(request.getLocation().getLatitude());
            entity.setLongitude(request.getLocation().getLongitude());
        }

        // Set interests
        if (request.getInterests() != null) {
            entity.setInterests(StudentProfileDTO.convertInterestsToString(request.getInterests()));
        }
    }

    /**
     * Convert entity to DTO
     * @param entity Entity to convert
     * @return DTO representation
     */
    private StudentProfileDTO entityToDTO(StudentProfileEntity entity) {
        StudentProfileDTO dto = new StudentProfileDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUser().getUserId());
        dto.setBio(entity.getBio());
        dto.setGradeLevel(entity.getGradeLevel());
        dto.setSchool(entity.getSchool());

        // Set location
        LocationDTO locationDTO = new LocationDTO(
                entity.getCity(),
                entity.getState(),
                entity.getCountry(),
                entity.getLatitude(),
                entity.getLongitude()
        );
        dto.setLocation(locationDTO);

        // Set interests
        dto.setInterests(StudentProfileDTO.convertInterestsToList(entity.getInterests()));

        // Set timestamps
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        return dto;
    }
} 