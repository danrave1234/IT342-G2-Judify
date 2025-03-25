package edu.cit.Judify.User;

import edu.cit.Judify.User.DTO.AuthenticatedUserDTO;
import edu.cit.Judify.User.DTO.UserDTO;
import edu.cit.Judify.User.DTO.UserDTOMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final UserDTOMapper userDTOMapper;

    @Autowired
    public UserController(UserService userService, UserDTOMapper userDTOMapper) {
        this.userService = userService;
        this.userDTOMapper = userDTOMapper;
    }

    @PostMapping("/addUser")
    public ResponseEntity<UserDTO> createUser(@RequestBody UserEntity user) {
        return ResponseEntity.ok(userDTOMapper.toDTO(userService.createUser(user)));
    }

    @GetMapping("/findById/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(userDTOMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/findByEmail/{email}")
    public ResponseEntity<UserDTO> getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(userDTOMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/getAllUsers")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers()
                .stream()
                .map(userDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/findByRole/{role}")
    public ResponseEntity<List<UserDTO>> getUsersByRole(@PathVariable String role) {
        return ResponseEntity.ok(userService.getUsersByRole(role)
                .stream()
                .map(userDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @PutMapping("/updateUser/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id,
            @RequestBody UserEntity userDetails) {
        return ResponseEntity.ok(userDTOMapper.toDTO(userService.updateUser(id, userDetails)));
    }

    @DeleteMapping("/deleteUser/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/updateRole/{id}")
    public ResponseEntity<UserDTO> updateUserRole(
            @PathVariable Long id,
            @RequestBody String role) {
        return ResponseEntity.ok(userDTOMapper.toDTO(userService.updateUserRole(id, role)));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticatedUserDTO> authenticateUser(
            @RequestParam String email,
            @RequestParam String password) {
        return ResponseEntity.ok(userService.authenticateUser(email, password));
    }
} 