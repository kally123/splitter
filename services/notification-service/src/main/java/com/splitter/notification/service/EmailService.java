package com.splitter.notification.service;

import com.splitter.notification.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.mail.internet.MimeMessage;
import java.util.Map;

/**
 * Service for sending emails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    /**
     * Send an email notification.
     */
    public Mono<Boolean> sendEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        return Mono.fromCallable(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setTo(to);
                helper.setSubject(subject);
                helper.setFrom("noreply@splitter.app");

                Context context = new Context();
                context.setVariables(variables);
                String htmlContent = templateEngine.process(templateName, context);
                helper.setText(htmlContent, true);

                mailSender.send(message);
                log.info("Email sent successfully to: {}", to);
                return true;
            } catch (Exception e) {
                log.error("Failed to send email to: {}", to, e);
                return false;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Send a simple notification email.
     */
    public Mono<Boolean> sendNotificationEmail(String to, Notification notification) {
        Map<String, Object> variables = Map.of(
                "title", notification.getTitle(),
                "message", notification.getMessage(),
                "type", notification.getType().name()
        );
        return sendEmail(to, notification.getTitle(), "notification", variables);
    }

    /**
     * Send expense added notification.
     */
    public Mono<Boolean> sendExpenseAddedEmail(String to, String expenseDescription, 
            String amount, String groupName, String addedBy) {
        Map<String, Object> variables = Map.of(
                "expenseDescription", expenseDescription,
                "amount", amount,
                "groupName", groupName,
                "addedBy", addedBy
        );
        return sendEmail(to, "New expense added: " + expenseDescription, "expense-added", variables);
    }

    /**
     * Send settlement request notification.
     */
    public Mono<Boolean> sendSettlementRequestEmail(String to, String fromUser, 
            String amount, String groupName) {
        Map<String, Object> variables = Map.of(
                "fromUser", fromUser,
                "amount", amount,
                "groupName", groupName
        );
        return sendEmail(to, "Settlement request from " + fromUser, "settlement-request", variables);
    }

    /**
     * Send group invitation email.
     */
    public Mono<Boolean> sendGroupInvitationEmail(String to, String groupName, 
            String invitedBy, String inviteLink) {
        Map<String, Object> variables = Map.of(
                "groupName", groupName,
                "invitedBy", invitedBy,
                "inviteLink", inviteLink
        );
        return sendEmail(to, "You've been invited to join " + groupName, "group-invitation", variables);
    }
}
