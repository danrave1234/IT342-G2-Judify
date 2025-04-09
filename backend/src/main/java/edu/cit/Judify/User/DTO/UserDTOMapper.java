package edu.cit.Judify.User.DTO;

import edu.cit.Judify.User.UserEntity;
import edu.cit.Judify.User.UserRole;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class UserDTOMapper {

    public UserDTO toDTO(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        UserDTO dto = new UserDTO();
        dto.setUserId(entity.getUserId());
        dto.setUsername(entity.getUsername());
        dto.setEmail(entity.getEmail());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setRole(entity.getRole());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public UserEntity toEntity(UserDTO dto) {
        if (dto == null) {
            return null;
        }

        UserEntity entity = new UserEntity();
        entity.setUserId(dto.getUserId());
        entity.setUsername(dto.getUsername());
        entity.setEmail(dto.getEmail());
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setRole(dto.getRole());
        // Note: Password and other sensitive fields should be handled separately
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        return entity;
    }
    
    public UserEntity toEntity(CreateUserDTO dto) {
        if (dto == null) {
            return null;
        }

        UserEntity entity = new UserEntity();
        entity.setUsername(dto.getUsername());
        entity.setEmail(dto.getEmail());
        entity.setPassword(dto.getPassword()); // Will be hashed by the service
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setContactDetails(dto.getContactDetails());
        entity.setRole(dto.getRole() != null ? dto.getRole() : UserRole.STUDENT); // Default to STUDENT if not provided
        
        // The entity constructor already sets createdAt
        entity.setUpdatedAt(new Date());
        
        return entity;
    }
} 