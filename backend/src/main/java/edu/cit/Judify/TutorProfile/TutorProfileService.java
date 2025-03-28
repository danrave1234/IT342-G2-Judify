package edu.cit.Judify.TutorProfile;

import edu.cit.Judify.TutorProfile.DTO.TutorProfileDTO;
import edu.cit.Judify.TutorProfile.DTO.TutorProfileDTOMapper;
import edu.cit.Judify.TutorSubject.TutorSubjectService;
import edu.cit.Judify.User.UserEntity;
import edu.cit.Judify.User.UserRepository;
import edu.cit.Judify.User.UserRole;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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
} 
