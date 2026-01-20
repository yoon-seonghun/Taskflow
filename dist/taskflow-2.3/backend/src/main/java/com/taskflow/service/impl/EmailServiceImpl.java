package com.taskflow.service.impl;

import com.taskflow.domain.EmailLog;
import com.taskflow.domain.Item;
import com.taskflow.domain.SystemConfig;
import com.taskflow.domain.User;
import com.taskflow.dto.email.EmailSendRequest;
import com.taskflow.mapper.EmailLogMapper;
import com.taskflow.mapper.ItemMapper;
import com.taskflow.mapper.UserMapper;
import com.taskflow.service.EmailService;
import com.taskflow.service.SystemConfigService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 이메일 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final EmailLogMapper emailLogMapper;
    private final UserMapper userMapper;
    private final ItemMapper itemMapper;
    private final SystemConfigService systemConfigService;

    @Override
    @Transactional
    public List<EmailLog> sendEmail(EmailSendRequest request, String senderUsername) {
        List<EmailLog> results = new ArrayList<>();

        for (EmailSendRequest.Recipient recipient : request.getRecipients()) {
            EmailLog emailLog = EmailLog.builder()
                    .emailType(EmailLog.TYPE_MANUAL)
                    .subject(request.getSubject())
                    .recipientEmail(recipient.getEmail())
                    .recipientName(recipient.getName())
                    .content(request.getContent())
                    .status(EmailLog.STATUS_PENDING)
                    .relatedType(request.getRelatedType())
                    .relatedId(request.getRelatedId())
                    .createdBy(senderUsername)
                    .build();

            emailLogMapper.insert(emailLog);
            sendEmailAsync(emailLog);
            results.add(emailLog);
        }

        return results;
    }

    @Override
    @Transactional
    public EmailLog sendTaskAssignNotification(Long itemId, String assigneeUsername, String senderUsername) {
        User assignee = userMapper.findByUsername(assigneeUsername).orElse(null);
        if (assignee == null || assignee.getEmail() == null || assignee.getEmail().isEmpty()) {
            log.warn("Cannot send task assign notification: no email for user {}", assigneeUsername);
            return null;
        }

        Item item = itemMapper.findById(itemId).orElse(null);
        if (item == null) {
            log.warn("Cannot send task assign notification: item {} not found", itemId);
            return null;
        }

        String subject = "[TaskFlow] 새 업무가 배정되었습니다: " + item.getTitle();
        String content = buildTaskAssignContent(item, assignee);

        EmailLog emailLog = EmailLog.builder()
                .emailType(EmailLog.TYPE_TASK_ASSIGN)
                .subject(subject)
                .recipientEmail(assignee.getEmail())
                .recipientName(assignee.getName())
                .recipientUsername(assigneeUsername)
                .content(content)
                .status(EmailLog.STATUS_PENDING)
                .relatedType(EmailLog.RELATED_ITEM)
                .relatedId(itemId)
                .createdBy(senderUsername)
                .build();

        emailLogMapper.insert(emailLog);
        sendEmailAsync(emailLog);

        return emailLog;
    }

    @Override
    @Transactional
    public EmailLog sendTaskTransferNotification(Long itemId, String targetUsername, String senderUsername) {
        User target = userMapper.findByUsername(targetUsername).orElse(null);
        if (target == null || target.getEmail() == null || target.getEmail().isEmpty()) {
            log.warn("Cannot send task transfer notification: no email for user {}", targetUsername);
            return null;
        }

        Item item = itemMapper.findById(itemId).orElse(null);
        if (item == null) {
            log.warn("Cannot send task transfer notification: item {} not found", itemId);
            return null;
        }

        String subject = "[TaskFlow] 업무가 이관되었습니다: " + item.getTitle();
        String content = buildTaskTransferContent(item, target, senderUsername);

        EmailLog emailLog = EmailLog.builder()
                .emailType(EmailLog.TYPE_TASK_TRANSFER)
                .subject(subject)
                .recipientEmail(target.getEmail())
                .recipientName(target.getName())
                .recipientUsername(targetUsername)
                .content(content)
                .status(EmailLog.STATUS_PENDING)
                .relatedType(EmailLog.RELATED_ITEM)
                .relatedId(itemId)
                .createdBy(senderUsername)
                .build();

        emailLogMapper.insert(emailLog);
        sendEmailAsync(emailLog);

        return emailLog;
    }

    @Override
    @Transactional
    public EmailLog sendTaskShareNotification(Long itemId, String targetUsername, String senderUsername) {
        User target = userMapper.findByUsername(targetUsername).orElse(null);
        if (target == null || target.getEmail() == null || target.getEmail().isEmpty()) {
            log.warn("Cannot send task share notification: no email for user {}", targetUsername);
            return null;
        }

        Item item = itemMapper.findById(itemId).orElse(null);
        if (item == null) {
            log.warn("Cannot send task share notification: item {} not found", itemId);
            return null;
        }

        String subject = "[TaskFlow] 업무가 공유되었습니다: " + item.getTitle();
        String content = buildTaskShareContent(item, target, senderUsername);

        EmailLog emailLog = EmailLog.builder()
                .emailType(EmailLog.TYPE_TASK_SHARE)
                .subject(subject)
                .recipientEmail(target.getEmail())
                .recipientName(target.getName())
                .recipientUsername(targetUsername)
                .content(content)
                .status(EmailLog.STATUS_PENDING)
                .relatedType(EmailLog.RELATED_ITEM)
                .relatedId(itemId)
                .createdBy(senderUsername)
                .build();

        emailLogMapper.insert(emailLog);
        sendEmailAsync(emailLog);

        return emailLog;
    }

    @Override
    @Transactional
    public List<EmailLog> sendDueDateAlerts() {
        // 마감일 알림 배치 처리 - 별도 구현 필요
        // 마감일이 오늘/내일인 업무 조회 후 담당자에게 알림
        log.info("Due date alerts batch job started");
        return new ArrayList<>();
    }

    @Override
    public boolean testSmtpConnection() {
        try {
            JavaMailSender mailSender = createMailSender();
            if (mailSender instanceof JavaMailSenderImpl) {
                ((JavaMailSenderImpl) mailSender).testConnection();
                log.info("SMTP connection test successful");
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("SMTP connection test failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public EmailLog sendTestEmail(String recipientEmail, String senderUsername) {
        String subject = "[TaskFlow] 테스트 메일";
        String content = buildTestEmailContent();

        EmailLog emailLog = EmailLog.builder()
                .emailType(EmailLog.TYPE_MANUAL)
                .subject(subject)
                .recipientEmail(recipientEmail)
                .content(content)
                .status(EmailLog.STATUS_PENDING)
                .createdBy(senderUsername)
                .build();

        emailLogMapper.insert(emailLog);

        // 테스트 메일은 동기 발송
        try {
            doSendEmail(emailLog);
            emailLog.markAsSent();
            emailLogMapper.updateStatus(
                    emailLog.getEmailLogId(),
                    emailLog.getStatus(),
                    null,
                    emailLog.getSentAt()
            );
        } catch (Exception e) {
            emailLog.markAsFailed(e.getMessage());
            emailLogMapper.updateStatus(
                    emailLog.getEmailLogId(),
                    emailLog.getStatus(),
                    emailLog.getErrorMessage(),
                    null
            );
        }

        return emailLog;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmailLog> getEmailLogs(int page, int size) {
        int offset = page * size;
        return emailLogMapper.selectAll(offset, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmailLog> getEmailLogsByStatus(String status, int page, int size) {
        int offset = page * size;
        return emailLogMapper.selectByStatus(status, offset, size);
    }

    @Override
    @Transactional(readOnly = true)
    public int getTotalEmailLogCount() {
        return emailLogMapper.countAll();
    }

    @Override
    @Transactional
    public EmailLog resendEmail(Long emailLogId, String senderUsername) {
        EmailLog originalLog = emailLogMapper.selectById(emailLogId);
        if (originalLog == null) {
            throw new IllegalArgumentException("이메일 로그를 찾을 수 없습니다: " + emailLogId);
        }

        EmailLog newLog = EmailLog.builder()
                .emailType(originalLog.getEmailType())
                .subject(originalLog.getSubject())
                .recipientEmail(originalLog.getRecipientEmail())
                .recipientName(originalLog.getRecipientName())
                .recipientUsername(originalLog.getRecipientUsername())
                .content(originalLog.getContent())
                .status(EmailLog.STATUS_PENDING)
                .relatedType(originalLog.getRelatedType())
                .relatedId(originalLog.getRelatedId())
                .createdBy(senderUsername)
                .build();

        emailLogMapper.insert(newLog);
        sendEmailAsync(newLog);

        return newLog;
    }

    // ========================================
    // Private Methods
    // ========================================

    @Async
    protected void sendEmailAsync(EmailLog emailLog) {
        try {
            doSendEmail(emailLog);
            emailLog.markAsSent();
            emailLogMapper.updateStatus(
                    emailLog.getEmailLogId(),
                    emailLog.getStatus(),
                    null,
                    emailLog.getSentAt()
            );
            log.info("Email sent successfully to {}", emailLog.getRecipientEmail());
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", emailLog.getRecipientEmail(), e.getMessage());
            emailLog.markAsFailed(e.getMessage());
            emailLogMapper.updateStatus(
                    emailLog.getEmailLogId(),
                    emailLog.getStatus(),
                    emailLog.getErrorMessage(),
                    null
            );
        }
    }

    private void doSendEmail(EmailLog emailLog) throws MessagingException {
        JavaMailSender mailSender = createMailSender();
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        String fromAddress = systemConfigService.getConfigValue(
                SystemConfig.GROUP_SMTP,
                SystemConfig.SMTP_FROM_ADDRESS,
                "TaskFlow <noreply@taskflow.com>"
        );

        helper.setFrom(fromAddress);
        helper.setTo(emailLog.getRecipientEmail());
        helper.setSubject(emailLog.getSubject());
        helper.setText(emailLog.getContent(), true);

        mailSender.send(message);
    }

    private JavaMailSender createMailSender() {
        Map<String, String> smtpConfig = systemConfigService.getSmtpConfig();

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(smtpConfig.get(SystemConfig.SMTP_HOST));
        mailSender.setPort(Integer.parseInt(smtpConfig.getOrDefault(SystemConfig.SMTP_PORT, "587")));
        mailSender.setUsername(smtpConfig.get(SystemConfig.SMTP_USERNAME));
        mailSender.setPassword(smtpConfig.get(SystemConfig.SMTP_PASSWORD));

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");

        String securityType = smtpConfig.getOrDefault(SystemConfig.SMTP_SECURITY_TYPE, SystemConfig.SECURITY_STARTTLS);
        if (SystemConfig.SECURITY_STARTTLS.equals(securityType)) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        } else if (SystemConfig.SECURITY_TLS.equals(securityType)) {
            props.put("mail.smtp.ssl.enable", "true");
        }

        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        return mailSender;
    }

    private String buildTaskAssignContent(Item item, User assignee) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h2>새 업무가 배정되었습니다</h2>
                <p>안녕하세요, %s님</p>
                <p>다음 업무가 배정되었습니다:</p>
                <div style="background: #f5f5f5; padding: 15px; border-radius: 5px; margin: 15px 0;">
                    <strong>업무명:</strong> %s<br>
                    <strong>마감일:</strong> %s<br>
                </div>
                <p>자세한 내용은 TaskFlow에서 확인해주세요.</p>
                <hr>
                <p style="color: #888; font-size: 12px;">이 메일은 TaskFlow 시스템에서 자동 발송되었습니다.</p>
            </body>
            </html>
            """,
                assignee.getName(),
                item.getTitle(),
                item.getDueDate() != null ? item.getDueDate().toString() : "미지정"
        );
    }

    private String buildTaskTransferContent(Item item, User target, String senderUsername) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h2>업무가 이관되었습니다</h2>
                <p>안녕하세요, %s님</p>
                <p>%s님이 다음 업무를 이관했습니다:</p>
                <div style="background: #f5f5f5; padding: 15px; border-radius: 5px; margin: 15px 0;">
                    <strong>업무명:</strong> %s<br>
                    <strong>마감일:</strong> %s<br>
                </div>
                <p>자세한 내용은 TaskFlow에서 확인해주세요.</p>
                <hr>
                <p style="color: #888; font-size: 12px;">이 메일은 TaskFlow 시스템에서 자동 발송되었습니다.</p>
            </body>
            </html>
            """,
                target.getName(),
                senderUsername,
                item.getTitle(),
                item.getDueDate() != null ? item.getDueDate().toString() : "미지정"
        );
    }

    private String buildTaskShareContent(Item item, User target, String senderUsername) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h2>업무가 공유되었습니다</h2>
                <p>안녕하세요, %s님</p>
                <p>%s님이 다음 업무를 공유했습니다:</p>
                <div style="background: #f5f5f5; padding: 15px; border-radius: 5px; margin: 15px 0;">
                    <strong>업무명:</strong> %s<br>
                </div>
                <p>자세한 내용은 TaskFlow에서 확인해주세요.</p>
                <hr>
                <p style="color: #888; font-size: 12px;">이 메일은 TaskFlow 시스템에서 자동 발송되었습니다.</p>
            </body>
            </html>
            """,
                target.getName(),
                senderUsername,
                item.getTitle()
        );
    }

    private String buildTestEmailContent() {
        return """
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h2>TaskFlow 테스트 메일</h2>
                <p>이 메일이 정상적으로 수신되었다면 SMTP 설정이 올바르게 구성된 것입니다.</p>
                <div style="background: #e8f5e9; padding: 15px; border-radius: 5px; margin: 15px 0;">
                    <strong>상태:</strong> ✅ SMTP 연결 성공
                </div>
                <hr>
                <p style="color: #888; font-size: 12px;">이 메일은 TaskFlow 시스템에서 발송되었습니다.</p>
            </body>
            </html>
            """;
    }
}
