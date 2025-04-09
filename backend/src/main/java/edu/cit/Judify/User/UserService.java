package edu.cit.Judify.User;

import edu.cit.Judify.User.DTO.AuthenticatedUserDTO;
import edu.cit.Judify.User.DTO.UserDTO;
import edu.cit.Judify.User.DTO.UserDTOMapper;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, UserDTOMapper userDTOMapper, 
                       Key jwtSecretKey, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userDTOMapper = userDTOMapper;
        this.jwtSecretKey = jwtSecretKey;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserEntity createUser(UserEntity user) {
        // Hash the password before saving
        String plainPassword = user.getPassword();
        String hashedPassword = passwordEncoder.encode(plainPassword);
        user.setPassword(hashedPassword);
        return userRepository.save(user);
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
    public UserEntity updateUser(Long id, UserDTO userDTO) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update basic user information
        if (userDTO.getUsername() != null) user.setUsername(userDTO.getUsername());
        if (userDTO.getEmail() != null) user.setEmail(userDTO.getEmail());
        if (userDTO.getFirstName() != null) user.setFirstName(userDTO.getFirstName());
        if (userDTO.getLastName() != null) user.setLastName(userDTO.getLastName());
        if (userDTO.getProfilePicture() != null) user.setProfilePicture(userDTO.getProfilePicture());
        if (userDTO.getContactDetails() != null) user.setContactDetails(userDTO.getContactDetails());
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

        try {
            // Convert the role string to uppercase and trim any whitespace
            UserRole userRole = UserRole.valueOf(role.trim().toUpperCase());
            user.setRole(userRole);
            user.setUpdatedAt(new Date());
            return userRepository.save(user);
        } catch (IllegalArgumentException e) {
            // Throw a more informative exception if the role is invalid
            throw new IllegalArgumentException("Invalid role: " + role + ". Valid roles are: STUDENT, TUTOR, ADMIN");
        }
    }

    public AuthenticatedUserDTO authenticateUser(String email, String password) {
        AuthenticatedUserDTO authDTO = new AuthenticatedUserDTO();
        authDTO.setAuthenticated(false); // Default to not authenticated

        // First, find the user by email
        Optional<UserEntity> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();

            // Use the password encoder to match the passwords
            if (password != null && passwordEncoder.matches(password, user.getPassword())) {
                // Authentication successful
                authDTO.setAuthenticated(true);
                authDTO.setUserId(user.getUserId());
                authDTO.setUsername(user.getUsername());
                authDTO.setEmail(user.getEmail());
                authDTO.setFirstName(user.getFirstName());
                authDTO.setLastName(user.getLastName());
                authDTO.setRole(user.getRole());

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