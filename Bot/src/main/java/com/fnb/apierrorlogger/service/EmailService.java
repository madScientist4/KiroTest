package com.fnb.apierrorlogger.service;

import com.fnb.apierrorlogger.model.EmailLog;
import com.fnb.apierrorlogger.model.ErrorRequest;
import com.fnb.apierrorlogger.model.ValidationResult;
import com.fnb.apierrorlogger.repository.EmailLogRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for sending email notifications about validated API errors.
 * Implements Requirements 3.1, 3.2, 3.4, 3.5
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;
    
    @Value("${app.email.from}")
    private String fromEmail;
    
    @Value("${app.email.investigation-team}")
    private String investigationTeamEmail;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Sends error notification email to the investigation team.
     * Only called when validation passes.
     * 
     * @param errorRequest The error request to notify about
     * @param validationResult The validation result
     * @return true if email was sent successfully, false otherwise
     */
    @Transactional
    public boolean sendErrorNotification(ErrorRequest errorRequest, ValidationResult validationResult) {
        log.info("Attempting to send email notification for error request: {}", errorRequest.getId());
        
        EmailLog emailLog = EmailLog.builder()
                .errorRequestId(errorRequest.getId())
                .recipient(investigationTeamEmail)
                .sentAt(LocalDateTime.now())
                .build();
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(investigationTeamEmail);
            helper.setSubject("API Error Notification - " + errorRequest.getApiEndpoint());
            helper.setText(formatEmailContent(errorRequest, validationResult), true);
            
            mailSender.send(message);
            
            emailLog.setDeliveryStatus("sent");
            emailLogRepository.save(emailLog);
            
            log.info("Email notification sent successfully for error request: {}", errorRequest.getId());
            return true;
            
        } catch (MessagingException e) {
            log.error("Failed to send email notification for error request: {}", errorRequest.getId(), e);
            
            emailLog.setDeliveryStatus("failed");
            emailLog.setErrorMessage(e.getMessage());
            emailLogRepository.save(emailLog);
            
            return false;
        } catch (Exception e) {
            log.error("Unexpected error while sending email for error request: {}", errorRequest.getId(), e);
            
            emailLog.setDeliveryStatus("failed");
            emailLog.setErrorMessage("Unexpected error: " + e.getMessage());
            emailLogRepository.save(emailLog);
            
            return false;
        }
    }
    
    /**
     * Formats the email content with all error details.
     * Includes: endpoint, method, payload, status, body, timestamp, validation results
     * 
     * @param errorRequest The error request
     * @param validationResult The validation result
     * @return Formatted HTML email content
     */
    private String formatEmailContent(ErrorRequest errorRequest, ValidationResult validationResult) {
        StringBuilder content = new StringBuilder();
        
        content.append("<html><body style='font-family: Arial, sans-serif;'>");
        content.append("<h2 style='color: #d32f2f;'>API Error Notification</h2>");
        content.append("<p>A validated API error has been logged and requires investigation.</p>");
        
        content.append("<h3>Error Details</h3>");
        content.append("<table style='border-collapse: collapse; width: 100%;'>");
        
        addTableRow(content, "Endpoint", errorRequest.getApiEndpoint());
        addTableRow(content, "HTTP Method", errorRequest.getHttpMethod());
        addTableRow(content, "Response Status", String.valueOf(errorRequest.getResponseStatus()));
        addTableRow(content, "Timestamp", errorRequest.getTimestamp().format(DATE_FORMATTER));
        addTableRow(content, "Environment", errorRequest.getEnvironment());
        
        content.append("</table>");
        
        content.append("<h3>Request Payload</h3>");
        content.append("<pre style='background-color: #f5f5f5; padding: 10px; border-radius: 4px; overflow-x: auto;'>");
        content.append(escapeHtml(errorRequest.getRequestPayload()));
        content.append("</pre>");
        
        content.append("<h3>Response Body</h3>");
        content.append("<pre style='background-color: #f5f5f5; padding: 10px; border-radius: 4px; overflow-x: auto;'>");
        content.append(escapeHtml(errorRequest.getResponseBody()));
        content.append("</pre>");
        
        content.append("<h3>Validation Results</h3>");
        content.append("<p style='color: #4caf50; font-weight: bold;'>✓ Validation Passed</p>");
        
        if (validationResult.getWarnings() != null && !validationResult.getWarnings().isEmpty()) {
            content.append("<h4>Warnings:</h4>");
            content.append("<ul>");
            for (String warning : validationResult.getWarnings()) {
                content.append("<li>").append(escapeHtml(warning)).append("</li>");
            }
            content.append("</ul>");
        }
        
        content.append("<hr style='margin-top: 20px;'>");
        content.append("<p style='color: #666; font-size: 12px;'>This is an automated notification from the API Error Logger system.</p>");
        content.append("</body></html>");
        
        return content.toString();
    }
    
    /**
     * Adds a table row to the email content.
     */
    private void addTableRow(StringBuilder content, String label, String value) {
        content.append("<tr>");
        content.append("<td style='border: 1px solid #ddd; padding: 8px; font-weight: bold; background-color: #f9f9f9;'>");
        content.append(label);
        content.append("</td>");
        content.append("<td style='border: 1px solid #ddd; padding: 8px;'>");
        content.append(escapeHtml(value));
        content.append("</td>");
        content.append("</tr>");
    }
    
    /**
     * Escapes HTML special characters to prevent injection.
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
