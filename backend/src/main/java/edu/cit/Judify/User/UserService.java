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
import java.util.Map;
import java.util.Optional;
import java.util.Arrays;

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

    public String generateJwtToken(UserEntity user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getUserId())
                .claim("role", user.getRole().name())
                .setIssuedAt(new Date())
                .signWith(jwtSecretKey)
                .compact();
    }

    /**
     * Finds or creates a user based on OAuth2 authentication data
     * 
     * @param email The user's email from OAuth2 provider
     * @param name The user's name from OAuth2 provider
     * @param attributes All OAuth2 attributes
     * @return The user entity (either existing or newly created)
     */
    public UserEntity findOrCreateOAuth2User(String email, String name, Map<String, Object> attributes) {
        // Check if user already exists
        Optional<UserEntity> existingUser = userRepository.findByEmail(email);
        
        if (existingUser.isPresent()) {
            return existingUser.get();
        }
        
        // Create new user
        UserEntity newUser = new UserEntity();
        newUser.setEmail(email);
        
        // Handle default values to avoid null constraint violations
        String username = email != null ? email.split("@")[0] : "user";
        newUser.setUsername(username);
        
        // Extract first name and last name from attributes if available
        // Try different attribute names that Google might return
        if (attributes.containsKey("given_name")) {
            newUser.setFirstName((String) attributes.get("given_name"));
        } else if (attributes.containsKey("givenName")) {
            newUser.setFirstName((String) attributes.get("givenName"));
        }
        
        if (attributes.containsKey("family_name")) {
            newUser.setLastName((String) attributes.get("family_name"));
        } else if (attributes.containsKey("familyName")) {
            newUser.setLastName((String) attributes.get("familyName"));
        }
        
        // If first_name and last_name are still null, use name field or defaults
        if (newUser.getFirstName() == null) {
            if (name != null && !name.trim().isEmpty()) {
                String[] nameParts = name.split(" ");
                if (nameParts.length > 0) {
                    newUser.setFirstName(nameParts[0]);
                    if (nameParts.length > 1) {
                        newUser.setLastName(String.join(" ", Arrays.copyOfRange(nameParts, 1, nameParts.length)));
                    }
                }
            } else {
                // Default values as fallback
                newUser.setFirstName(username); // Use username as default first name
            }
        }
        
        // Ensure last_name has a value if it's also required
        if (newUser.getLastName() == null) {
            newUser.setLastName(""); // Empty string as default last name
        }
        
        // Set default values
        newUser.setPassword(""); // OAuth2 users don't need a password
        newUser.setRole(UserRole.STUDENT); // Default role
        
        // Set timestamps
        Date now = new Date();
        newUser.setCreatedAt(now);
        newUser.setUpdatedAt(now);
        
        // Debug log
        System.out.println("Creating OAuth2 user: " + newUser.getEmail() + 
                          ", FirstName: " + newUser.getFirstName() + 
                          ", LastName: " + newUser.getLastName());
        
        // Save the new user
        return userRepository.save(newUser);
    }
} 
