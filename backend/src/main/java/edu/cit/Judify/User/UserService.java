package edu.cit.Judify.User;

import edu.cit.Judify.User.DTO.AuthenticatedUserDTO;
import edu.cit.Judify.User.DTO.UserDTO;
import edu.cit.Judify.User.DTO.UserDTOMapper;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserDTOMapper userDTOMapper;
    private final Key jwtSecretKey;

    @Autowired
    public UserService(UserRepository userRepository, UserDTOMapper userDTOMapper, Key jwtSecretKey) {
        this.userRepository = userRepository;
        this.userDTOMapper = userDTOMapper;
        this.jwtSecretKey = jwtSecretKey;
    }

    @Transactional
    public UserEntity createUser(UserEntity user) {
        // Add logging to debug the user entity being received
        System.out.println("Creating user with data: " + user.getEmail() + ", " + user.getUsername() + ", role: " + user.getRole());
        System.out.println("Password field present: " + (user.getPassword() != null ? "Yes" : "No"));

        // Use the validation method to check all required fields
        if (!user.validate()) {
            throw new IllegalArgumentException("User validation failed - check server logs for details");
        }

        // Ensure dates are set
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(new Date());
        }

        if (user.getUpdatedAt() == null) {
            user.setUpdatedAt(new Date());
        }

        try {
            // Save the user and log the result
            UserEntity savedUser = userRepository.save(user);
            System.out.println("User created successfully with ID: " + savedUser.getUserId());
            return savedUser;
        } catch (Exception e) {
            System.err.println("Error saving user to database: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public Optional<UserEntity> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<UserEntity> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    public List<UserEntity> getUsersByRole(String role) {
        return userRepository.findByRole(UserRole.valueOf(role));
    }

    @Transactional
    public UserEntity updateUser(Long id, UserEntity userDetails) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setUsername(userDetails.getUsername());
        user.setEmail(userDetails.getEmail());
        user.setFirstName(userDetails.getFirstName());
        user.setLastName(userDetails.getLastName());
        user.setProfilePicture(userDetails.getProfilePicture());
        user.setContactDetails(userDetails.getContactDetails());
        user.setUpdatedAt(new Date());

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public UserEntity updateUserRole(Long id, String role) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRole(UserRole.valueOf(role));
        user.setUpdatedAt(new Date());

        return userRepository.save(user);
    }

    public AuthenticatedUserDTO authenticateUser(String email, String password) {
        AuthenticatedUserDTO authDTO = new AuthenticatedUserDTO();
        authDTO.setAuthenticated(false); // Default to not authenticated

        // First, find the user by email
        Optional<UserEntity> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();

            // Simple password check (since we've removed hashing)
            if (password != null && password.equals(user.getPassword())) {
                // Authentication successful
                authDTO.setAuthenticated(true);
                authDTO.setUserId(user.getUserId());
                authDTO.setUsername(user.getUsername());
                authDTO.setEmail(user.getEmail());
                authDTO.setFirstName(user.getFirstName());
                authDTO.setLastName(user.getLastName());
                authDTO.setRole(user.getRole());
                authDTO.setProfilePicture(user.getProfilePicture());

                // Generate JWT token if needed
                String token = generateJwtToken(user);
                authDTO.setToken(token);
            }
        }

        return authDTO;
    }

    private String generateJwtToken(UserEntity user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getUserId())
                .claim("role", user.getRole().name())
                .setIssuedAt(new Date())
                .signWith(jwtSecretKey)
                .compact();
    }
} 
