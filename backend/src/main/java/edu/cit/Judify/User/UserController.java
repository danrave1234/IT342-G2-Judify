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

import java.util.List;
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
        return ResponseEntity.ok(userDTOMapper.toDTO(userService.createUser(user)));
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
} 
