package edu.cit.Judify.TutorProfile;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cit.Judify.TutorProfile.DTO.TutorProfileDTO;
import edu.cit.Judify.TutorProfile.DTO.TutorProfileDTOMapper;
import edu.cit.Judify.TutorProfile.DTO.TutorRegistrationDTO;
import edu.cit.Judify.TutorSubject.TutorSubjectService;
import edu.cit.Judify.User.UserEntity;
import edu.cit.Judify.User.UserRepository;
import edu.cit.Judify.User.UserRole;
import jakarta.persistence.EntityNotFoundException;

@Service
public class TutorProfileService {

    private final TutorProfileRepository tutorProfileRepository;
    private final UserRepository userRepository;
    private final TutorProfileDTOMapper dtoMapper;
    private final TutorSubjectService tutorSubjectService;

    @Autowired
    public TutorProfileService(TutorProfileRepository tutorProfileRepository, 
                             UserRepository userRepository,
                             TutorProfileDTOMapper dtoMapper,
                             TutorSubjectService tutorSubjectService) {
        this.tutorProfileRepository = tutorProfileRepository;
        this.userRepository = userRepository;
        this.dtoMapper = dtoMapper;
        this.tutorSubjectService = tutorSubjectService;
    }

    public List<TutorProfileDTO> getAllTutorProfiles() {
        return tutorProfileRepository.findAll().stream()
                .map(dtoMapper::toDTO)
                .collect(Collectors.toList());
    }

    public TutorProfileDTO getTutorProfileById(Long id) {
        TutorProfileEntity profile = tutorProfileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TutorProfile not found with id: " + id));
        return dtoMapper.toDTO(profile);
    }

    public TutorProfileDTO getTutorProfileByUserId(Long userId) {
        TutorProfileEntity profile = tutorProfileRepository.findByUserUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("TutorProfile not found for user id: " + userId));
        return dtoMapper.toDTO(profile);
    }

    /**
     * Get the userId for a given tutorId
     * @param tutorId The tutor profile ID
     * @return The associated user ID
     * @throws EntityNotFoundException if tutor profile not found
     */
    public Long getUserIdFromTutorId(Long tutorId) {
        TutorProfileEntity profile = tutorProfileRepository.findById(tutorId)
                .orElseThrow(() -> new EntityNotFoundException("TutorProfile not found with id: " + tutorId));
        return profile.getUser().getUserId();
    }
    
    /**
     * Get the tutorId for a given userId
     * @param userId The user ID
     * @return The associated tutor profile ID
     * @throws EntityNotFoundException if tutor profile not found for this user
     */
    public Long getTutorIdFromUserId(Long userId) {
        TutorProfileEntity profile = tutorProfileRepository.findByUserUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("TutorProfile not found for user id: " + userId));
        return profile.getId();
    }

    @Transactional
    public TutorProfileDTO createTutorProfile(TutorProfileDTO dto) {
        UserEntity user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + dto.getUserId()));

        // Update the user's role to TUTOR
        user.setRole(UserRole.TUTOR);
        user.setUpdatedAt(new Date());
        userRepository.save(user);

        TutorProfileEntity entity = dtoMapper.toEntity(dto);
        entity.setUser(user);

        // First save the profile without subjects to get an ID
        TutorProfileEntity savedEntity = tutorProfileRepository.save(entity);

        // Then add subjects if any are provided (they will be added using the helper method)
        if (dto.getSubjects() != null && !dto.getSubjects().isEmpty()) {
            // The setSubjects helper method will handle creating the subject entities
            savedEntity.setSubjects(dto.getSubjects());
            savedEntity = tutorProfileRepository.save(savedEntity);
        }

        return dtoMapper.toDTO(savedEntity);
    }

    @Transactional
    public TutorProfileDTO updateTutorProfile(Long id, TutorProfileDTO dto) {
        TutorProfileEntity existingProfile = tutorProfileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TutorProfile not found with id: " + id));

        // Update fields
        existingProfile.setBiography(dto.getBio());
        existingProfile.setExpertise(dto.getExpertise());
        existingProfile.setHourlyRate(dto.getHourlyRate());
        existingProfile.setRating(dto.getRating());
        existingProfile.setTotalReviews(dto.getTotalReviews());

        // Update subjects if provided
        if (dto.getSubjects() != null) {
            existingProfile.setSubjects(dto.getSubjects());
        }

        TutorProfileEntity updatedEntity = tutorProfileRepository.save(existingProfile);
        return dtoMapper.toDTO(updatedEntity);
    }

    @Transactional
    public void deleteTutorProfile(Long id) {
        if (!tutorProfileRepository.existsById(id)) {
            throw new EntityNotFoundException("TutorProfile not found with id: " + id);
        }
        // The cascade delete will handle removing the related subjects
        tutorProfileRepository.deleteById(id);
    }

    public List<TutorProfileDTO> searchTutorProfiles(String subject) {
        return tutorProfileRepository.findBySubjectName(subject).stream()
                .map(dtoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TutorProfileDTO updateTutorRating(Long id, Double newRating) {
        TutorProfileEntity profile = tutorProfileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TutorProfile not found with id: " + id));

        profile.setRating(newRating);
        profile.setTotalReviews(profile.getTotalReviews() + 1);

        TutorProfileEntity updatedEntity = tutorProfileRepository.save(profile);
        return dtoMapper.toDTO(updatedEntity);
    }

    // New method for paginated and filtered tutor profiles
    /**
     * Update a tutor's location
     * 
     * @param id Tutor profile ID
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @return Updated tutor profile DTO
     * @throws EntityNotFoundException if tutor profile not found
     */
    @Transactional
    public TutorProfileDTO updateTutorLocation(Long id, Double latitude, Double longitude) {
        TutorProfileEntity profile = tutorProfileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TutorProfile not found with id: " + id));

        profile.setLatitude(latitude);
        profile.setLongitude(longitude);

        TutorProfileEntity updatedEntity = tutorProfileRepository.save(profile);
        return dtoMapper.toDTO(updatedEntity);
    }

    public Page<TutorProfileDTO> getAllTutorProfilesPaginated(int page, int size, 
                                                           String expertise, 
                                                           Double minRate, 
                                                           Double maxRate, 
                                                           Double minRating) {
        Pageable pageable = PageRequest.of(page, size);

        // Build dynamic query based on provided filters
        Page<TutorProfileEntity> profiles;

        if (expertise != null || minRate != null || maxRate != null || minRating != null) {
            // Create specifications based on filters
            Specification<TutorProfileEntity> spec = Specification.where(null);

            if (expertise != null && !expertise.isEmpty()) {
                spec = spec.and((root, query, cb) -> 
                    cb.like(cb.lower(root.get("expertise")), "%" + expertise.toLowerCase() + "%"));
            }

            if (minRate != null) {
                spec = spec.and((root, query, cb) -> 
                    cb.greaterThanOrEqualTo(root.get("hourlyRate"), minRate));
            }

            if (maxRate != null) {
                spec = spec.and((root, query, cb) -> 
                    cb.lessThanOrEqualTo(root.get("hourlyRate"), maxRate));
            }

            if (minRating != null) {
                spec = spec.and((root, query, cb) -> 
                    cb.greaterThanOrEqualTo(root.get("rating"), minRating));
            }

            profiles = tutorProfileRepository.findAll(spec, pageable);
        } else {
            profiles = tutorProfileRepository.findAll(pageable);
        }

        return profiles.map(dtoMapper::toDTO);
    }

    /**
     * Get a list of random tutor profiles
     * 
     * @param limit Maximum number of tutor profiles to return (default is 10)
     * @return List of random tutor profiles
     */
    public List<TutorProfileDTO> getRandomTutorProfiles(int limit) {
        // Limit the number of tutors to avoid performance issues
        if (limit <= 0 || limit > 10) {
            limit = 10;
        }

        // Get all tutor profiles
        List<TutorProfileEntity> allProfiles = tutorProfileRepository.findAll();

        // Shuffle the list to get random tutors
        java.util.Collections.shuffle(allProfiles);

        // Take only the first 'limit' elements
        List<TutorProfileEntity> randomProfiles = allProfiles.stream()
                .limit(limit)
                .collect(Collectors.toList());


        // Convert to DTOs
        return randomProfiles.stream()
                .map(dtoMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**

     * Register a new user as a tutor
     * 
     * @param registrationDTO DTO containing user and tutor profile information
     * @return The created tutor profile DTO
     */
    @Transactional
    public TutorProfileDTO registerTutor(TutorRegistrationDTO registrationDTO) {
        // Create a new user entity
        UserEntity user = new UserEntity();
        user.setUsername(registrationDTO.getUsername());
        user.setEmail(registrationDTO.getEmail());
        user.setPassword(registrationDTO.getPassword());
        user.setFirstName(registrationDTO.getFirstName());
        user.setLastName(registrationDTO.getLastName());
        user.setContactDetails(registrationDTO.getContactDetails());
        user.setRole(UserRole.TUTOR);

        // Save the user
        UserEntity savedUser = userRepository.save(user);

        // Create a new tutor profile entity
        TutorProfileEntity tutorProfile = new TutorProfileEntity();
        tutorProfile.setUser(savedUser);
        tutorProfile.setBiography(registrationDTO.getBio());
        tutorProfile.setExpertise(registrationDTO.getExpertise());
        tutorProfile.setHourlyRate(registrationDTO.getHourlyRate());
        tutorProfile.setLatitude(registrationDTO.getLatitude());
        tutorProfile.setLongitude(registrationDTO.getLongitude());

        // Save the tutor profile
        TutorProfileEntity savedProfile = tutorProfileRepository.save(tutorProfile);

        // Add subjects if provided
        if (registrationDTO.getSubjects() != null && !registrationDTO.getSubjects().isEmpty()) {
            savedProfile.setSubjects(registrationDTO.getSubjects());
            savedProfile = tutorProfileRepository.save(savedProfile);
        }

        return dtoMapper.toDTO(savedProfile);
    }

    public Optional<TutorProfileEntity> findByUserId(Long userId) {
        return tutorProfileRepository.findByUserUserId(userId);

     * Update tutor location with full details including city, state, country
     * @param profileId tutor profile ID
     * @param latitude latitude
     * @param longitude longitude
     * @param city city name
     * @param state state name
     * @param country country name
     * @param shareLocation whether to share location
     * @return updated TutorProfileEntity
     */
    public TutorProfileEntity updateTutorLocation(
            Long profileId,
            Double latitude,
            Double longitude,
            String city,
            String state,
            String country,
            boolean shareLocation) {
        
        log.info("Updating tutor location: profileId={}, lat={}, lng={}, city={}, state={}, country={}, share={}",
                profileId, latitude, longitude, city, state, country, shareLocation);
        
        TutorProfileEntity profile = tutorProfileRepository.findById(profileId)
                .orElseThrow(() -> new EntityNotFoundException("Tutor profile not found with ID: " + profileId));
        
        profile.setLatitude(latitude);
        profile.setLongitude(longitude);
        profile.setCity(city);
        profile.setState(state);
        profile.setCountry(country);
        profile.setShareLocation(shareLocation);
        
        // Save the updated profile
        TutorProfileEntity updatedProfile = tutorProfileRepository.save(profile);
        log.info("Tutor location updated successfully for profile ID: {}", profileId);
        
        return updatedProfile;
    }

    /**
     * Clear tutor location
     * @param profileId tutor profile ID
     */
    public void clearTutorLocation(Long profileId) {
        log.info("Clearing tutor location for profile ID: {}", profileId);
        
        TutorProfileEntity profile = tutorProfileRepository.findById(profileId)
                .orElseThrow(() -> new EntityNotFoundException("Tutor profile not found with ID: " + profileId));
        
        // Clear all location data
        profile.setLatitude(null);
        profile.setLongitude(null);
        profile.setCity(null);
        profile.setState(null);
        profile.setCountry(null);
        profile.setShareLocation(false);
        
        // Save the updated profile
        tutorProfileRepository.save(profile);
        log.info("Tutor location cleared successfully for profile ID: {}", profileId);

    }
} 
