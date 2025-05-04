package edu.cit.Judify.Calendar;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter; // Ensure the dependency 'com.google.api-client' is properly configured in your build file.
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.api.services.calendar.model.Events;

import edu.cit.Judify.TutorAvailability.TutorAvailabilityEntity;
import edu.cit.Judify.TutoringSession.TutoringSessionEntity;
import edu.cit.Judify.User.UserEntity;

@Service
public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "Judify Tutoring Platform";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    @Value("${google.calendar.enabled:false}")
    private boolean calendarEnabled;

    /**
     * Creates an authorized Credential object.
     * 
     * @param userId The user ID to create credentials for
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private Credential getCredentials(String userId, final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = GoogleCalendarService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH + "/" + userId)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     * Gets a Calendar service instance for a specific user.
     * 
     * @param userId The user ID to get the Calendar service for
     * @return A Calendar service instance
     * @throws IOException If an error occurs
     * @throws GeneralSecurityException If a security error occurs
     */
    private Calendar getCalendarService(String userId) throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(userId, HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Checks if a time slot is available in the user's Google Calendar.
     * 
     * @param tutor The tutor entity
     * @param date The date to check
     * @param startTime The start time to check
     * @param endTime The end time to check
     * @return true if the time slot is available, false otherwise
     */
    public boolean isTimeSlotAvailable(UserEntity tutor, LocalDate date, String startTime, String endTime) {
        if (!calendarEnabled) {
            // If calendar integration is disabled, assume all slots are available
            return true;
        }

        try {
            // Convert date and time strings to DateTime objects
            LocalTime start = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime end = LocalTime.parse(endTime, DateTimeFormatter.ofPattern("HH:mm"));

            LocalDateTime startDateTime = LocalDateTime.of(date, start);
            LocalDateTime endDateTime = LocalDateTime.of(date, end);

            DateTime startDt = new DateTime(Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant()));
            DateTime endDt = new DateTime(Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant()));

            // Get events from Google Calendar
            Calendar service = getCalendarService(tutor.getUserId().toString());
            Events events = service.events().list("primary")
                    .setTimeMin(startDt)
                    .setTimeMax(endDt)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            // If there are any events during this time slot, it's not available
            return events.getItems().isEmpty();
        } catch (Exception e) {
            // Log the error and assume the slot is available
            System.err.println("Error checking calendar availability: " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }

    /**
     * Gets available time slots for a tutor on a specific date, based on their availability settings
     * and existing Google Calendar events.
     * 
     * @param tutor The tutor entity
     * @param date The date to check
     * @param availabilities The tutor's availability settings
     * @param durationMinutes The duration of the session in minutes
     * @return A list of available time slots
     */
    public List<TimeSlot> getAvailableTimeSlots(UserEntity tutor, LocalDate date, 
            List<TutorAvailabilityEntity> availabilities, int durationMinutes) {

        List<TimeSlot> availableSlots = new ArrayList<>();

        // Get day of week for the given date
        String dayOfWeek = date.getDayOfWeek().toString();

        // Filter availabilities for the given day of week
        List<TutorAvailabilityEntity> dayAvailabilities = new ArrayList<>();
        for (TutorAvailabilityEntity availability : availabilities) {
            if (availability.getDayOfWeek().equalsIgnoreCase(dayOfWeek)) {
                dayAvailabilities.add(availability);
            }
        }

        // For each availability window, generate 30-minute slots
        for (TutorAvailabilityEntity availability : dayAvailabilities) {
            LocalTime startTime = LocalTime.parse(availability.getStartTime(), DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime endTime = LocalTime.parse(availability.getEndTime(), DateTimeFormatter.ofPattern("HH:mm"));

            // Generate slots in 30-minute increments
            LocalTime currentTime = startTime;
            while (currentTime.plusMinutes(durationMinutes).isBefore(endTime) || 
                   currentTime.plusMinutes(durationMinutes).equals(endTime)) {

                LocalTime slotEndTime = currentTime.plusMinutes(durationMinutes);

                // Check if this slot is available in Google Calendar
                if (isTimeSlotAvailable(tutor, date, 
                        currentTime.format(DateTimeFormatter.ofPattern("HH:mm")), 
                        slotEndTime.format(DateTimeFormatter.ofPattern("HH:mm")))) {

                    availableSlots.add(new TimeSlot(
                        currentTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                        slotEndTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                    ));
                }

                // Move to next 30-minute slot
                currentTime = currentTime.plusMinutes(30);
            }
        }

        return availableSlots;
    }

    /**
     * Creates a Google Calendar event for a tutoring session.
     * 
     * @param session The tutoring session entity
     * @return The created event ID
     */
    public String createCalendarEvent(TutoringSessionEntity session) {
        if (!calendarEnabled) {
            return "calendar-disabled";
        }

        try {
            // Create event
            Event event = new Event()
                .setSummary("Tutoring Session: " + session.getSubject())
                .setDescription("Tutoring session with " + 
                    session.getStudent().getFirstName() + " " + session.getStudent().getLastName() + 
                    "\n\nSubject: " + session.getSubject() + 
                    (session.getNotes() != null && !session.getNotes().isEmpty() ? "\n\nNotes: " + session.getNotes() : ""));

            // Set start and end times
            DateTime startDateTime = new DateTime(session.getStartTime());
            EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime);
            event.setStart(start);

            DateTime endDateTime = new DateTime(session.getEndTime());
            EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime);
            event.setEnd(end);

            // Set location or meeting link
            if (session.getLocationData() != null && !session.getLocationData().isEmpty()) {
                event.setLocation(session.getLocationData());
            } else if (session.getMeetingLink() != null && !session.getMeetingLink().isEmpty()) {
                event.setLocation("Online: " + session.getMeetingLink());
                // Add conferencing data if available
                // This would require additional setup for Google Meet integration
            }

            // Add attendees
            List<EventAttendee> attendees = new ArrayList<>();

            EventAttendee tutorAttendee = new EventAttendee()
                .setEmail(session.getTutor().getEmail())
                .setDisplayName(session.getTutor().getFirstName() + " " + session.getTutor().getLastName())
                .setResponseStatus("accepted");
            attendees.add(tutorAttendee);

            EventAttendee studentAttendee = new EventAttendee()
                .setEmail(session.getStudent().getEmail())
                .setDisplayName(session.getStudent().getFirstName() + " " + session.getStudent().getLastName())
                .setResponseStatus("needsAction");
            attendees.add(studentAttendee);

            event.setAttendees(attendees);

            // Add reminders
            List<EventReminder> reminderOverrides = new ArrayList<>();
            EventReminder emailReminder = new EventReminder().setMethod("email").setMinutes(60);
            EventReminder popupReminder = new EventReminder().setMethod("popup").setMinutes(15);
            reminderOverrides.add(emailReminder);
            reminderOverrides.add(popupReminder);

            Event.Reminders reminders = new Event.Reminders()
                .setUseDefault(false)
                .setOverrides(reminderOverrides);
            event.setReminders(reminders);

            // Insert the event
            Calendar service = getCalendarService(session.getTutor().getUserId().toString());
            event = service.events().insert("primary", event).execute();

            return event.getId();
        } catch (Exception e) {
            System.err.println("Error creating calendar event: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Updates a Google Calendar event for a tutoring session.
     * 
     * @param session The tutoring session entity
     * @param eventId The event ID to update
     * @return The updated event ID
     */
    public String updateCalendarEvent(TutoringSessionEntity session, String eventId) {
        if (!calendarEnabled || eventId == null) {
            return null;
        }

        try {
            // Get the existing event
            Calendar service = getCalendarService(session.getTutor().getUserId().toString());
            Event event = service.events().get("primary", eventId).execute();

            // Update event details
            event.setSummary("Tutoring Session: " + session.getSubject())
                .setDescription("Tutoring session with " + 
                    session.getStudent().getFirstName() + " " + session.getStudent().getLastName() + 
                    "\n\nSubject: " + session.getSubject() + 
                    (session.getNotes() != null && !session.getNotes().isEmpty() ? "\n\nNotes: " + session.getNotes() : ""));

            // Update start and end times
            DateTime startDateTime = new DateTime(session.getStartTime());
            EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime);
            event.setStart(start);

            DateTime endDateTime = new DateTime(session.getEndTime());
            EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime);
            event.setEnd(end);

            // Update location or meeting link
            if (session.getLocationData() != null && !session.getLocationData().isEmpty()) {
                event.setLocation(session.getLocationData());
            } else if (session.getMeetingLink() != null && !session.getMeetingLink().isEmpty()) {
                event.setLocation("Online: " + session.getMeetingLink());
            }

            // Update the event
            event = service.events().update("primary", eventId, event).execute();

            return event.getId();
        } catch (Exception e) {
            System.err.println("Error updating calendar event: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Deletes a Google Calendar event.
     * 
     * @param userId The user ID
     * @param eventId The event ID to delete
     * @return true if successful, false otherwise
     */
    public boolean deleteCalendarEvent(String userId, String eventId) {
        if (!calendarEnabled) {
            return true;
        }

        try {
            // Delete the event
            Calendar service = getCalendarService(userId);
            service.events().delete("primary", eventId).execute();
            return true;
        } catch (Exception e) {
            // Log the error
            System.err.println("Error deleting calendar event: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Checks if a user has connected their Google Calendar account.
     * 
     * @param user The user entity to check
     * @return true if the user has connected their Google Calendar, false otherwise
     */
    public boolean isCalendarConnected(UserEntity user) {
        if (!calendarEnabled) {
            return false;
        }

        try {
            // Try to get the calendar service - if successful, the user is connected
            Calendar service = getCalendarService(user.getUserId().toString());
            
            // Try a simple operation to verify the connection
            service.calendarList().list().setMaxResults(1).execute();
            return true;
        } catch (Exception e) {
            // Log the error
            System.err.println("Calendar is not connected for user " + user.getUserId() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Simple class to represent a time slot.
     */
    public static class TimeSlot {
        private String startTime;
        private String endTime;

        public TimeSlot(String startTime, String endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }
    }
}
