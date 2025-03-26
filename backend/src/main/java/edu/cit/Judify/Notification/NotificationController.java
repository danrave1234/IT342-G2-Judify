package edu.cit.Judify.Notification;

import edu.cit.Judify.Notification.DTO.NotificationDTO;
import edu.cit.Judify.Notification.DTO.NotificationDTOMapper;
import edu.cit.Judify.User.UserEntity;
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
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationDTOMapper notificationDTOMapper;

    @Autowired
    public NotificationController(NotificationService notificationService, NotificationDTOMapper notificationDTOMapper) {
        this.notificationService = notificationService;
        this.notificationDTOMapper = notificationDTOMapper;
    }

    @Operation(summary = "Create a new notification", description = "Creates a new notification for a user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notification successfully created",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping("/createNotification")
    public ResponseEntity<NotificationDTO> createNotification(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Notification data to create", required = true)
            @RequestBody NotificationDTO notificationDTO) {
        NotificationEntity notification = notificationDTOMapper.toEntity(notificationDTO, null); // User will be set by the service
        return ResponseEntity.ok(notificationDTOMapper.toDTO(notificationService.createNotification(notification)));
    }

    @Operation(summary = "Get notification by ID", description = "Returns a notification by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the notification"),
        @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @GetMapping("/findById/{id}")
    public ResponseEntity<NotificationDTO> getNotificationById(
            @Parameter(description = "Notification ID") @PathVariable Long id) {
        return notificationService.getNotificationById(id)
                .map(notificationDTOMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get user's notifications", description = "Returns all notifications for a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user's notifications")
    })
    @GetMapping("/findByUser/{userId}")
    public ResponseEntity<List<NotificationDTO>> getUserNotifications(
            @Parameter(description = "User ID") @PathVariable UserEntity user) {
        return ResponseEntity.ok(notificationService.getUserNotifications(user)
                .stream()
                .map(notificationDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get user's unread notifications", description = "Returns all unread notifications for a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user's unread notifications")
    })
    @GetMapping("/findUnreadByUser/{userId}")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications(
            @Parameter(description = "User ID") @PathVariable UserEntity user) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(user)
                .stream()
                .map(notificationDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get notifications by type", description = "Returns all notifications of a specific type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved notifications by type")
    })
    @GetMapping("/findByType/{type}")
    public ResponseEntity<List<NotificationDTO>> getNotificationsByType(
            @Parameter(description = "Notification type (e.g., SESSION_REMINDER, NEW_MESSAGE)") @PathVariable String type) {
        return ResponseEntity.ok(notificationService.getNotificationsByType(type)
                .stream()
                .map(notificationDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Mark notification as read", description = "Marks a specific notification as read")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notification successfully marked as read"),
        @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @PutMapping("/markAsRead/{id}")
    public ResponseEntity<NotificationDTO> markAsRead(
            @Parameter(description = "Notification ID") @PathVariable Long id) {
        return ResponseEntity.ok(notificationDTOMapper.toDTO(notificationService.markAsRead(id)));
    }

    @Operation(summary = "Mark all notifications as read", description = "Marks all notifications for a specific user as read")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All notifications successfully marked as read")
    })
    @PutMapping("/markAllAsRead/{userId}")
    public ResponseEntity<Void> markAllAsRead(
            @Parameter(description = "User ID") @PathVariable UserEntity user) {
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete a notification", description = "Deletes a notification by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notification successfully deleted"),
        @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @DeleteMapping("/deleteNotification/{id}")
    public ResponseEntity<Void> deleteNotification(
            @Parameter(description = "Notification ID") @PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete all user notifications", description = "Deletes all notifications for a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All user notifications successfully deleted")
    })
    @DeleteMapping("/deleteAllForUser/{userId}")
    public ResponseEntity<Void> deleteAllUserNotifications(
            @Parameter(description = "User ID") @PathVariable UserEntity user) {
        notificationService.deleteAllUserNotifications(user);
        return ResponseEntity.ok().build();
    }
} 