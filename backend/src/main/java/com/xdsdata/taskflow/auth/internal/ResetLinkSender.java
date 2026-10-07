package com.xdsdata.taskflow.auth.internal;

import java.time.Duration;

import com.xdsdata.taskflow.common.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Delivers password-reset links. Uses SMTP when a mail server is configured (SPRING_MAIL_HOST); otherwise
 * the link is written to the log, which is enabled for local development only (APP_LOG_RESET_LINKS).
 * Sending is asynchronous so response time does not reveal whether an account exists.
 */
@Component
class ResetLinkSender {

	private static final Logger log = LoggerFactory.getLogger(ResetLinkSender.class);

	private final ObjectProvider<JavaMailSender> mailSender;

	private final AppProperties properties;

	ResetLinkSender(ObjectProvider<JavaMailSender> mailSender, AppProperties properties) {
		this.mailSender = mailSender;
		this.properties = properties;
	}

	@Async
	public void send(String recipientName, String recipientEmail, String link, Duration validity) {
		JavaMailSender sender = mailSender.getIfAvailable();
		if (sender != null) {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(properties.passwordReset().mailFrom());
			message.setTo(recipientEmail);
			message.setSubject("Reset your TaskFlow password");
			message.setText("""
					Hello %s,

					We received a request to reset your TaskFlow password. Open this link to choose a new one:

					%s

					The link expires in %d minutes and can be used once. If you didn't ask for this, you can ignore this email.
					""".formatted(recipientName, link, validity.toMinutes()));
			try {
				sender.send(message);
			}
			catch (MailException ex) {
				log.error("Could not send the password reset email to {}", recipientEmail, ex);
			}
			return;
		}
		if (properties.passwordReset().logLinks()) {
			log.info("Password reset link for {} (development only, no mail server configured): {}", recipientEmail, link);
		}
		else {
			log.warn("Password reset requested for {} but no mail server is configured (set SPRING_MAIL_HOST).",
					recipientEmail);
		}
	}

}
