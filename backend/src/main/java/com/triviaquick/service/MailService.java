package com.triviaquick.service;

import com.triviaquick.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envío de correos transaccionales. Con SMTP configurado (MAIL_HOST) se envía de verdad;
 * sin él (desarrollo) el enlace se escribe en el log del servidor para poder probar el flujo.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> sender;
    private final String host;
    private final AppProperties.Mail props;

    public MailService(ObjectProvider<JavaMailSender> sender, @Value("${spring.mail.host:}") String host,
                       AppProperties props) {
        this.sender = sender;
        this.host = host;
        this.props = props.mail();
    }

    public String verificationLink(String token) {
        return props.publicUrl().replaceAll("/$", "") + "/verify-email?token=" + token;
    }

    /** Devuelve true si el correo salió por SMTP; false si solo se registró en el log (modo desarrollo). */
    public boolean sendEmailVerification(String to, String name, String token) {
        String link = verificationLink(token);
        String body = "Hola " + name + ",\n\n"
                + "Para confirmar este correo como tu nuevo correo de TriviaQuick, abre este enlace (válido 30 minutos):\n\n"
                + link + "\n\nSi no lo solicitaste, ignora este mensaje: tu correo actual no cambiará.\n\nTriviaQuick";
        JavaMailSender mailSender = sender.getIfAvailable();
        if (host == null || host.isBlank() || mailSender == null) {
            log.warn("SMTP no configurado (MAIL_HOST vacío). Enlace de verificación para {}: {}", mask(to), link);
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(props.from());
            message.setTo(to);
            message.setSubject("Confirma tu nuevo correo en TriviaQuick");
            message.setText(body);
            mailSender.send(message);
            return true;
        } catch (MailException e) {
            log.error("No se pudo enviar el correo de verificación: {}", e.getMessage());
            throw new IllegalStateException("No se pudo enviar el correo de verificación", e);
        }
    }

    private static String mask(String email) {
        int at = email.indexOf('@');
        return at <= 1 ? "***" + email.substring(Math.max(at, 0)) : email.charAt(0) + "***" + email.substring(at);
    }
}
