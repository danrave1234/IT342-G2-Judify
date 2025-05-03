package edu.cit.Judify.User;

import edu.cit.Judify.User.DTO.AuthenticatedUserDTO;
import edu.cit.Judify.User.DTO.UserDTO;
import edu.cit.Judify.User.DTO.UserDTOMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
@Tag(name = "User", description = "User management endpoints")
public class UserController {

    private final UserService userService;
    private final UserDTOMapper userDTOMapper;

    @Autowired
    public UserController(UserService userService, UserDTOMapper userDTOMapper) {
        this.userService = userService;
        this.userDTOMapper = userDTOMapper;
    }

    @Operation(summary = "Create a new user", description = "Creates a new user account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User successfully created",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping("/addUser")
    public ResponseEntity<UserDTO> createUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "User data to create", required = true)
            @RequestBody UserEntity user) {
        // Add debug logging to inspect the incoming request
        System.out.println("Received registration request:");
        System.out.println("Email: " + user.getEmail());
        System.out.println("Username: " + user.getUsername());
        System.out.println("FirstName: " + user.getFirstName());
        System.out.println("LastName: " + user.getLastName());
        System.out.println("Role: " + user.getRole());
        System.out.println("Password null check: " + (user.getPassword() == null ? "Password is NULL" : "Password is present"));
        System.out.println("Password empty check: " + (user.getPassword() != null && user.getPassword().trim().isEmpty() ? "Password is EMPTY" : "Password has content"));

        try {
            UserEntity createdUser = userService.createUser(user);
            return ResponseEntity.ok(userDTOMapper.toDTO(createdUser));
        } catch (Exception e) {
            System.err.println("Error creating user: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Operation(summary = "Get user by ID", description = "Returns a user by their ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/findById/{id}")
    public ResponseEntity<UserDTO> getUserById(
            @Parameter(description = "User ID") @PathVariable Long id) {
        return userService.getUserById(id)
                .map(userDTOMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get user by email", description = "Returns a user by their email address")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/findByEmail/{email}")
    public ResponseEntity<UserDTO> getUserByEmail(
            @Parameter(description = "User email") @PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(userDTOMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get user by email (query param)", description = "Returns a user by their email address using query parameter")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/find-by-email")
    public ResponseEntity<UserDTO> getUserByEmailQueryParam(
            @Parameter(description = "User email") @RequestParam String email) {
        return userService.getUserByEmail(email)
                .map(userDTOMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get all users", description = "Returns a list of all users")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved users list")
    })
    @GetMapping("/getAllUsers")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers()
                .stream()
                .map(userDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get users by role", description = "Returns a list of users with the specified role")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved users with the specified role")
    })
    @GetMapping("/findByRole/{role}")
    public ResponseEntity<List<UserDTO>> getUsersByRole(
            @Parameter(description = "User role") @PathVariable String role) {
        return ResponseEntity.ok(userService.getUsersByRole(role)
                .stream()
                .map(userDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Update user details", description = "Updates an existing user's details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User successfully updated"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/updateUser/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @Parameter(description = "User ID") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated user details", required = true)
            @RequestBody UserEntity userDetails) {
        return ResponseEntity.ok(userDTOMapper.toDTO(userService.updateUser(id, userDetails)));
    }

    @Operation(summary = "Delete user", description = "Deletes a user by their ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User successfully deleted"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/deleteUser/{id}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "User ID") @PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update user role", description = "Updates the role of an existing user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User role successfully updated"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/updateRole/{id}")
    public ResponseEntity<UserDTO> updateUserRole(
            @Parameter(description = "User ID") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "New user role", required = true)
            @RequestBody String role) {
        return ResponseEntity.ok(userDTOMapper.toDTO(userService.updateUserRole(id, role)));
    }

    @Operation(summary = "Authenticate user", description = "Authenticates a user using email and password")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication successful",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthenticatedUserDTO.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed - invalid credentials")
    })
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticatedUserDTO> authenticateUser(
            @Parameter(description = "User email", required = true) @RequestParam String email,
            @Parameter(description = "User password", required = true) @RequestParam String password) {
        return ResponseEntity.ok(userService.authenticateUser(email, password));
    }

    /**
     * Test endpoint for manual user creation with proper error handling
     */
    @PostMapping("/manual-user-create")
    public ResponseEntity<?> manualCreateUser(@RequestBody UserEntity userRequest) {
        try {
            // Log the incoming request
            System.out.println("Manual user creation request received:");
            System.out.println("Email: " + userRequest.getEmail());
            System.out.println("Username: " + userRequest.getUsername());
            System.out.println("Password field present: " + (userRequest.getPassword() != null));
            System.out.println("Role: " + userRequest.getRole());

            // Validate the user data
            boolean isValid = userRequest.validate();
            if (!isValid) {
                return ResponseEntity.badRequest().body("User validation failed - see server logs for details");
            }

            // Create the user
            UserEntity createdUser = userService.createUser(userRequest);
            return ResponseEntity.ok(userDTOMapper.toDTO(createdUser));
        } catch (Exception e) {
            System.err.println("Error in manual user creation: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Server error: " + e.getMessage());
        }
    }

    /**
     * Alternative registration endpoint that accepts a request body with a string role field
     * and converts it to the UserRole enum
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, Object> requestBody) {
        try {
            System.out.println("Registration request received: " + requestBody);

            // Extract values from the request
            String username = (String) requestBody.get("username");
            String email = (String) requestBody.get("email");
            String password = (String) requestBody.get("password");
            String firstName = (String) requestBody.get("firstName");
            String lastName = (String) requestBody.get("lastName");
            String roleStr = (String) requestBody.get("role");

            // Validate required fields
            if (username == null || email == null || password == null ||
                firstName == null || lastName == null || roleStr == null) {
                return ResponseEntity.badRequest()
                    .body("Missing required fields. All fields (username, email, password, firstName, lastName, role) are required.");
            }

            // Create the user entity
            UserEntity user = new UserEntity();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(password);
            user.setFirstName(firstName);
            user.setLastName(lastName);

            // Convert string role to enum
            try {
                UserRole role = UserRole.valueOf(roleStr);
                user.setRole(role);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body("Invalid role value. Must be one of: " + Arrays.toString(UserRole.values()));
            }

            // Set optional fields if present
            if (requestBody.containsKey("profilePicture")) {
                user.setProfilePicture((String) requestBody.get("profilePicture"));
            }

            if (requestBody.containsKey("contactDetails")) {
                user.setContactDetails((String) requestBody.get("contactDetails"));
            }

            // Set timestamps
            Date now = new Date();
            user.setCreatedAt(now);
            user.setUpdatedAt(now);

            // Create the user
            UserEntity createdUser = userService.createUser(user);
            return ResponseEntity.ok(userDTOMapper.toDTO(createdUser));
        } catch (Exception e) {
            System.err.println("Error in user registration: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Server error: " + e.getMessage());
        }
    }

    /**
     * Upload profile picture for a user
     */
    @Operation(summary = "Upload profile picture", description = "Uploads a profile picture for a user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile picture successfully uploaded"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/{userId}/profile-picture")
    public ResponseEntity<?> uploadProfilePicture(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Profile picture file") @RequestParam("file") MultipartFile file) {
        try {
            // Check if user exists
            return userService.getUserById(userId)
                    .map(user -> {
                        try {
                            // Convert the file to Base64 string
                            String base64Image = "data:" + file.getContentType() + ";base64," +
                                                java.util.Base64.getEncoder().encodeToString(file.getBytes());

                            // Update user's profile picture
                            user.setProfilePicture(base64Image);
                            user.setUpdatedAt(new Date());
                            UserEntity updatedUser = userService.updateUser(userId, user);

                            // Create response with both profilePicture and profileImage fields for frontend compatibility
                            Map<String, Object> response = new HashMap<>();
                            response.put("profilePicture", base64Image);
                            response.put("profileImage", base64Image);
                            response.put("message", "Profile picture updated successfully");

                            return ResponseEntity.ok(response);
                        } catch (Exception e) {
                            System.err.println("Error processing profile picture: " + e.getMessage());
                            e.printStackTrace();
                            return ResponseEntity.status(500).body("Error processing profile picture: " + e.getMessage());
                        }
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            System.err.println("Error uploading profile picture: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Server error: " + e.getMessage());
        }
    }

    @Operation(summary = "OAuth2 Authentication Success", description = "Endpoint to handle successful OAuth2 authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication successful - redirects to frontend")
    })
    @GetMapping("/oauth2-success")
    public ResponseEntity<Void> handleOAuth2Success(@AuthenticationPrincipal OAuth2User oauth2User, HttpServletRequest request) {
        // Extract user info from OAuth2 authentication
        Map<String, Object> attributes = oauth2User.getAttributes();

        // Debug log all attributes
        System.out.println("OAuth2 authentication success, received attributes:");
        attributes.forEach((key, value) -> System.out.println(key + ": " + value));

        // Extract email (main identifier)
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        System.out.println("OAuth2 user email: " + email);
        System.out.println("OAuth2 user name: " + name);

        // Find or create the user
        UserEntity user = userService.findOrCreateOAuth2User(email, name, attributes);

        // Create a JWT token (same as regular login)
        String token = userService.generateJwtToken(user);

        // Get the frontend URL for redirection
        String frontendUrl = getFrontendUrl(request);
        System.out.println("Redirecting to frontend URL: " + frontendUrl);

        // Redirect to frontend with token and userId
        return ResponseEntity.status(302)
            .header("Location", frontendUrl + "/auth/oauth2-callback?token=" + token + "&userId=" + user.getUserId())
            .build();
    }

    /**
     * Helper method to determine the frontend URL for redirection
     */
    private String getFrontendUrl(HttpServletRequest request) {
        // Default frontend URLs
        String localFrontendUrl = "http://localhost:5173";
        String productionFrontendUrl = "https://judify-web.vercel.app";
        
        // Check if we're running in a production environment
        boolean isProduction = false;
        String serverName = request.getServerName();
        
        if (serverName != null && !serverName.contains("localhost")) {
            isProduction = true;
        }
        
        // Use the appropriate frontend URL based on environment
        String frontendUrl = isProduction ? productionFrontendUrl : localFrontendUrl;
        
        System.out.println("Determined frontend URL: " + frontendUrl + " (isProduction: " + isProduction + ")");
        return frontendUrl;
    }

    @Operation(summary = "OAuth2 Authentication Failure", description = "Endpoint to handle failed OAuth2 authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "302", description = "Redirect to frontend login page with error")
    })
    @GetMapping("/oauth2-failure")
    public ResponseEntity<Void> handleOAuth2Failure(HttpServletRequest request) {
        // Get the frontend URL
        String frontendUrl = getFrontendUrl(request);

        // Redirect to frontend login page with error
        return ResponseEntity.status(302)
            .header("Location", frontendUrl + "/auth/login?error=oauth2_failure")
            .build();
    }

    @Operation(summary = "Check if user is new OAuth2 user", description = "Checks if a user needs to complete registration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status returned")
    })
    @GetMapping("/check-oauth2-status/{userId}")
    public ResponseEntity<Map<String, Object>> checkOAuth2Status(
            @Parameter(description = "User ID") @PathVariable Long userId) {

        Map<String, Object> response = new HashMap<>();

        try {
            Optional<UserEntity> userOpt = userService.getUserById(userId);

            if (userOpt.isPresent()) {
                UserEntity user = userOpt.get();

                // Debug logs to see what we're working with
                System.out.println("====== OAuth2 Status Check ======");
                System.out.println("User ID: " + user.getUserId());
                System.out.println("Email: " + user.getEmail());
                System.out.println("Username: " + user.getUsername());
                System.out.println("First Name: " + user.getFirstName());
                System.out.println("Last Name: " + user.getLastName());
                System.out.println("Password: " + (user.getPassword() == null ? "null" : (user.getPassword().isEmpty() ? "empty" : "has value")));
                System.out.println("Role: " + user.getRole());

                // Check if this is a new OAuth2 user who needs to complete registration
                // A user is considered to have completed registration if:
                // 1. They have a password set (meaning they registered normally, not via OAuth2), OR
                // 2. They have a role other than the default STUDENT role (meaning they chose their role), OR
                // 3. Their username is different from their email prefix (meaning they set a custom username)

                String emailPrefix = user.getEmail() != null && user.getEmail().contains("@")
                    ? user.getEmail().split("@")[0]
                    : "";

                boolean hasCustomUsername = user.getUsername() != null &&
                                          !user.getUsername().equals(emailPrefix) &&
                                          !user.getUsername().isEmpty();

                boolean hasPassword = user.getPassword() != null && !user.getPassword().isEmpty();
                boolean hasNonDefaultRole = user.getRole() != UserRole.STUDENT;

                boolean isNewOAuth2User = !hasPassword && !hasNonDefaultRole && !hasCustomUsername;

                // For debugging
                System.out.println("Email prefix: " + emailPrefix);
                System.out.println("Has custom username: " + hasCustomUsername);
                System.out.println("Has password: " + hasPassword);
                System.out.println("Has non-default role: " + hasNonDefaultRole);
                System.out.println("Is New OAuth2 User: " + isNewOAuth2User);
                System.out.println("Registration Complete: " + !isNewOAuth2User);
                System.out.println("================================");

                response.put("isNewOAuth2User", isNewOAuth2User);
                // Add registrationComplete field (opposite of isNewOAuth2User)
                response.put("registrationComplete", !isNewOAuth2User);
                response.put("role", user.getRole().name());
                response.put("email", user.getEmail());
                response.put("firstName", user.getFirstName());
                response.put("lastName", user.getLastName());
                response.put("username", user.getUsername());

                return ResponseEntity.ok(response);
            } else {
                response.put("error", "User not found");
                return ResponseEntity.status(404).body(response);
            }
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @Operation(summary = "Complete OAuth2 registration", description = "Updates a new OAuth2 user's details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Registration completed successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/complete-oauth2-registration/{userId}")
    public ResponseEntity<?> completeOAuth2Registration(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @RequestBody Map<String, Object> userData) {

        try {
            Optional<UserEntity> userOpt = userService.getUserById(userId);

            if (userOpt.isPresent()) {
                UserEntity user = userOpt.get();

                // Update user details
                if (userData.containsKey("username")) {
                    user.setUsername((String) userData.get("username"));
                }

                if (userData.containsKey("firstName")) {
                    user.setFirstName((String) userData.get("firstName"));
                }

                if (userData.containsKey("lastName")) {
                    user.setLastName((String) userData.get("lastName"));
                }

                // Update role if provided
                if (userData.containsKey("role")) {
                    String roleStr = (String) userData.get("role");
                    try {
                        UserRole role = UserRole.valueOf(roleStr);
                        user.setRole(role);
                    } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest()
                            .body("Invalid role value. Must be one of: " + Arrays.toString(UserRole.values()));
                    }
                }

                // Update the user
                user.setUpdatedAt(new Date());
                UserEntity updatedUser = userService.updateUser(userId, user);

                // Generate a new token with updated role
                String token = userService.generateJwtToken(updatedUser);

                // Return the updated user info and new token
                Map<String, Object> response = new HashMap<>();
                response.put("user", userDTOMapper.toDTO(updatedUser));
                response.put("token", token);

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Server error: " + e.getMessage());
        }
    }
}
