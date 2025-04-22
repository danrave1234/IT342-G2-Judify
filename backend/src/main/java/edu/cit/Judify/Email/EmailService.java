package edu.cit.Judify.Email;

import edu.cit.Judify.Calendar.CalendarService;
import edu.cit.Judify.TutoringSession.TutoringSessionEntity;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;

@Service
public class EmailService {

    private final JavaMailSender emailSender;
    private final CalendarService calendarService;
    
    @Value("${spring.mail.username:noreply@judify.edu}")
    private String fromEmail;
    
    @Autowired
    public EmailService(JavaMailSender emailSender, CalendarService calendarService) {
        this.emailSender = emailSender;
        this.calendarService = calendarService;
    }
    
    /**
     * Sends a session confirmation email with calendar attachment to both tutor and student
     * 
     * @param session The tutoring session
     * @throws MessagingException If there's an error sending the email
     * @throws IOException If there's an error generating the calendar file
     */
    public void sendSessionConfirmationEmail(TutoringSessionEntity session) throws MessagingException, IOException {
        // Generate calendar attachment
        byte[] calendarData = calendarService.generateICalendarFile(session);
        ByteArrayResource calendarAttachment = new ByteArrayResource(calendarData);
        
        // Format dates for display
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a");
        String formattedStartTime = dateFormat.format(session.getStartTime());
        String formattedEndTime = dateFormat.format(session.getEndTime());
        
        // Send email to student
        sendSessionEmailToRecipient(
            session.getStudent().getEmail(),
            session.getStudent().getFirstName(),
            session.getTutor().getFirstName() + " " + session.getTutor().getLastName(),
            session,
            formattedStartTime,
            formattedEndTime,
            calendarAttachment,
            false
        );
        
        // Send email to tutor
        sendSessionEmailToRecipient(
            session.getTutor().getEmail(),
            session.getTutor().getFirstName(),
            session.getStudent().getFirstName() + " " + session.getStudent().getLastName(),
            session,
            formattedStartTime,
            formattedEndTime,
            calendarAttachment,
            true
        );
    }
    
    /**
     * Sends a session confirmation email to a recipient (either tutor or student)
     */
    private void sendSessionEmailToRecipient(
            String toEmail, 
            String recipientFirstName,
            String otherPartyName,
            TutoringSessionEntity session,
            String formattedStartTime,
            String formattedEndTime,
            ByteArrayResource calendarAttachment,
            boolean isTutor) throws MessagingException {
        
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        
        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject("Tutoring Session Confirmation: " + session.getSubject());
        
        String emailContent = buildEmailContent(
            recipientFirstName,
            otherPartyName,
            session,
            formattedStartTime,
            formattedEndTime,
            isTutor
        );
        
        helper.setText(emailContent, true);
        helper.addAttachment("tutoring_session.ics", calendarAttachment);
        
        emailSender.send(message);
    }
    
    /**
     * Builds the HTML content for the email
     */
    private String buildEmailContent(
            String recipientFirstName,
            String otherPartyName,
            TutoringSessionEntity session,
            String formattedStartTime,
            String formattedEndTime,
            boolean isTutor) {
        
        StringBuilder builder = new StringBuilder();
        builder.append("<html><body>");
        builder.append("<h2>Tutoring Session Confirmation</h2>");
        builder.append("<p>Hello ").append(recipientFirstName).append(",</p>");
        
        if (isTutor) {
            builder.append("<p>A new tutoring session has been scheduled with your student, <strong>")
                  .append(otherPartyName).append("</strong>.</p>");
        } else {
            builder.append("<p>Your tutoring session with <strong>")
                  .append(otherPartyName).append("</strong> has been confirmed.</p>");
        }
        
        builder.append("<h3>Session Details:</h3>");
        builder.append("<ul>");
        builder.append("<li><strong>Subject:</strong> ").append(session.getSubject()).append("</li>");
        builder.append("<li><strong>Start Time:</strong> ").append(formattedStartTime).append("</li>");
        builder.append("<li><strong>End Time:</strong> ").append(formattedEndTime).append("</li>");
        
        if (session.getLocationData() != null && !session.getLocationData().isEmpty()) {
            builder.append("<li><strong>Location:</strong> ").append(session.getLocationData()).append("</li>");
        }
        
        if (session.getMeetingLink() != null && !session.getMeetingLink().isEmpty()) {
            builder.append("<li><strong>Meeting Link:</strong> <a href=\"")
                  .append(session.getMeetingLink()).append("\">")
                  .append(session.getMeetingLink()).append("</a></li>");
        }
        
        if (session.getNotes() != null && !session.getNotes().isEmpty()) {
            builder.append("<li><strong>Notes:</strong> ").append(session.getNotes()).append("</li>");
        }
        
        builder.append("</ul>");
        
        builder.append("<p>We've attached a calendar invitation that you can add to your calendar application.</p>");
        
        builder.append("<p>If you need to cancel or reschedule this session, please do so at least 12 hours in advance.</p>");
        
        builder.append("<p>Thank you for using Judify!</p>");
        builder.append("<p>The Judify Team</p>");
        builder.append("</body></html>");
        
        return builder.toString();
    }
}