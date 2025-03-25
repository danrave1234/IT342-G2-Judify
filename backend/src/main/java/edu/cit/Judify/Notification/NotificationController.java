package edu.cit.Judify.Notification;

import edu.cit.Judify.Notification.DTO.NotificationDTO;
import edu.cit.Judify.Notification.DTO.NotificationDTOMapper;
import edu.cit.Judify.User.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationDTOMapper notificationDTOMapper;

    @Autowired
    public NotificationController(NotificationService notificationService, NotificationDTOMapper notificationDTOMapper) {
        this.notificationService = notificationService;
        this.notificationDTOMapper = notificationDTOMapper;
    }

    @PostMapping("/createNotification")
    public ResponseEntity<NotificationDTO> createNotification(@RequestBody NotificationDTO notificationDTO) {
        NotificationEntity notification = notificationDTOMapper.toEntity(notificationDTO, null); // User will be set by the service
        return ResponseEntity.ok(notificationDTOMapper.toDTO(notificationService.createNotification(notification)));
    }

    @GetMapping("/findById/{id}")
    public ResponseEntity<NotificationDTO> getNotificationById(@PathVariable Long id) {
        return notificationService.getNotificationById(id)
                .map(notificationDTOMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/findByUser/{userId}")
    public ResponseEntity<List<NotificationDTO>> getUserNotifications(@PathVariable UserEntity user) {
        return ResponseEntity.ok(notificationService.getUserNotifications(user)
                .stream()
                .map(notificationDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/findUnreadByUser/{userId}")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications(@PathVariable UserEntity user) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(user)
                .stream()
                .map(notificationDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/findByType/{type}")
    public ResponseEntity<List<NotificationDTO>> getNotificationsByType(@PathVariable String type) {
        return ResponseEntity.ok(notificationService.getNotificationsByType(type)
                .stream()
                .map(notificationDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @PutMapping("/markAsRead/{id}")
    public ResponseEntity<NotificationDTO> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationDTOMapper.toDTO(notificationService.markAsRead(id)));
    }

    @PutMapping("/markAllAsRead/{userId}")
    public ResponseEntity<Void> markAllAsRead(@PathVariable UserEntity user) {
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/deleteNotification/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/deleteAllForUser/{userId}")
    public ResponseEntity<Void> deleteAllUserNotifications(@PathVariable UserEntity user) {
        notificationService.deleteAllUserNotifications(user);
        return ResponseEntity.ok().build();
    }
} 