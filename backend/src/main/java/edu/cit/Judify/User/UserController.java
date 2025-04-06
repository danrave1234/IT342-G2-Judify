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
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;

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
} 