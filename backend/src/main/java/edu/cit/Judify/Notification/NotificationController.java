package edu.cit.Judify.Notification;

import edu.cit.Judify.User.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<NotificationEntity> createNotification(@RequestBody NotificationEntity notification) {
        return ResponseEntity.ok(notificationService.createNotification(notification));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationEntity> getNotificationById(@PathVariable Long id) {
        return notificationService.getNotificationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationEntity>> getUserNotifications(@PathVariable UserEntity user) {
        return ResponseEntity.ok(notificationService.getUserNotifications(user));
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<NotificationEntity>> getUnreadNotifications(@PathVariable UserEntity user) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(user));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<NotificationEntity>> getNotificationsByType(@PathVariable String type) {
        return ResponseEntity.ok(notificationService.getNotificationsByType(type));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationEntity> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<Void> markAllAsRead(@PathVariable UserEntity user) {
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteAllUserNotifications(@PathVariable UserEntity user) {
        notificationService.deleteAllUserNotifications(user);
        return ResponseEntity.ok().build();
    }
} 