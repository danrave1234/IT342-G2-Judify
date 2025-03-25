package edu.cit.Judify.TutorProfile;

import edu.cit.Judify.TutorProfile.DTO.TutorProfileDTO;
import edu.cit.Judify.TutorProfile.DTO.TutorProfileDTOMapper;
import edu.cit.Judify.User.UserEntity;
import edu.cit.Judify.User.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TutorProfileService {

    private final TutorProfileRepository tutorProfileRepository;
    private final UserRepository userRepository;
    private final TutorProfileDTOMapper dtoMapper;

    @Autowired
    public TutorProfileService(TutorProfileRepository tutorProfileRepository, 
                             UserRepository userRepository,
                             TutorProfileDTOMapper dtoMapper) {
        this.tutorProfileRepository = tutorProfileRepository;
        this.userRepository = userRepository;
        this.dtoMapper = dtoMapper;
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

        TutorProfileEntity entity = dtoMapper.toEntity(dto);
        entity.setUser(user);
        
        TutorProfileEntity savedEntity = tutorProfileRepository.save(entity);
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
        existingProfile.setSubjects(dto.getSubjects());
        existingProfile.setRating(dto.getRating());
        existingProfile.setTotalReviews(dto.getTotalReviews());

        TutorProfileEntity updatedEntity = tutorProfileRepository.save(existingProfile);
        return dtoMapper.toDTO(updatedEntity);
    }

    @Transactional
    public void deleteTutorProfile(Long id) {
        if (!tutorProfileRepository.existsById(id)) {
            throw new EntityNotFoundException("TutorProfile not found with id: " + id);
        }
        tutorProfileRepository.deleteById(id);
    }

    public List<TutorProfileDTO> searchTutorProfiles(String subject) {
        return tutorProfileRepository.findBySubjectsContaining(subject).stream()
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
} 