package edu.cit.Judify.Calendar;

import edu.cit.Judify.TutoringSession.TutoringSessionEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Service
public class CalendarService {

    /**
     * Generates an iCalendar (.ics) file for a tutoring session
     * 
     * @param session The tutoring session
     * @return Byte array containing the iCalendar file content
     * @throws IOException If there's an error generating the file
     */
    public byte[] generateICalendarFile(TutoringSessionEntity session) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // Format dates according to iCalendar specification
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        
        // Generate a unique identifier for the event
        String uid = UUID.randomUUID().toString();
        
        // Build the iCalendar content
        StringBuilder builder = new StringBuilder();
        builder.append("BEGIN:VCALENDAR\r\n");
        builder.append("VERSION:2.0\r\n");
        builder.append("PRODID:-//Judify//Tutoring Session//EN\r\n");
        builder.append("CALSCALE:GREGORIAN\r\n");
        builder.append("METHOD:REQUEST\r\n");
        
        // Event details
        builder.append("BEGIN:VEVENT\r\n");
        builder.append("UID:").append(uid).append("\r\n");
        builder.append("DTSTAMP:").append(dateFormat.format(new Date())).append("\r\n");
        builder.append("DTSTART:").append(dateFormat.format(session.getStartTime())).append("\r\n");
        builder.append("DTEND:").append(dateFormat.format(session.getEndTime())).append("\r\n");
        builder.append("SUMMARY:Tutoring Session: ").append(session.getSubject()).append("\r\n");
        
        // Add description with session details
        StringBuilder description = new StringBuilder();
        description.append("Tutoring Session Details\\n");
        description.append("Subject: ").append(session.getSubject()).append("\\n");
        description.append("Tutor: ").append(session.getTutor().getFirstName()).append(" ")
                .append(session.getTutor().getLastName()).append("\\n");
        description.append("Student: ").append(session.getStudent().getFirstName()).append(" ")
                .append(session.getStudent().getLastName()).append("\\n");
        
        if (session.getNotes() != null && !session.getNotes().isEmpty()) {
            description.append("Notes: ").append(session.getNotes()).append("\\n");
        }
        
        if (session.getLocationData() != null && !session.getLocationData().isEmpty()) {
            description.append("Location: ").append(session.getLocationData()).append("\\n");
        }
        
        if (session.getMeetingLink() != null && !session.getMeetingLink().isEmpty()) {
            description.append("Meeting Link: ").append(session.getMeetingLink()).append("\\n");
        }
        
        builder.append("DESCRIPTION:").append(description.toString()).append("\r\n");
        
        // Add location if available
        if (session.getLocationData() != null && !session.getLocationData().isEmpty()) {
            builder.append("LOCATION:").append(session.getLocationData()).append("\r\n");
        } else if (session.getMeetingLink() != null && !session.getMeetingLink().isEmpty()) {
            builder.append("LOCATION:Online (").append(session.getMeetingLink()).append(")\r\n");
        }
        
        // Add organizer and attendees
        builder.append("ORGANIZER;CN=Judify:mailto:support@judify.edu\r\n");
        builder.append("ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE;CN=")
                .append(session.getTutor().getFirstName()).append(" ")
                .append(session.getTutor().getLastName())
                .append(":mailto:")
                .append(session.getTutor().getEmail())
                .append("\r\n");
        builder.append("ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE;CN=")
                .append(session.getStudent().getFirstName()).append(" ")
                .append(session.getStudent().getLastName())
                .append(":mailto:")
                .append(session.getStudent().getEmail())
                .append("\r\n");
        
        // Add reminder (15 minutes before)
        builder.append("BEGIN:VALARM\r\n");
        builder.append("ACTION:DISPLAY\r\n");
        builder.append("DESCRIPTION:Reminder: Tutoring Session\r\n");
        builder.append("TRIGGER:-PT15M\r\n");
        builder.append("END:VALARM\r\n");
        
        builder.append("END:VEVENT\r\n");
        builder.append("END:VCALENDAR\r\n");
        
        outputStream.write(builder.toString().getBytes());
        return outputStream.toByteArray();
    }
}