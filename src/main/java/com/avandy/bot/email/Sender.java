package com.avandy.bot.email;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class Sender {
    private final Properties properties;

    public Sender() {
        properties = new Properties();
    }

    public void send(String subject, String text, String fromEmail, String pwd, String toEmail) throws MessagingException {
        String host = getSmtp(fromEmail);
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "587");

        Session session = Session.getDefaultInstance(properties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, pwd);
            }
        });
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromEmail));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);
        message.setText(text);
        Transport.send(message);
    }

    // определение SMTP исходящей почты
    private String getSmtp(String fromEmail) {
        String smtp = "";
        String serviceName = fromEmail.substring(fromEmail.indexOf(64) + 1);
        switch (serviceName) {
            case "mail.ru":
            case "internet.ru":
            case "inbox.ru":
            case "list.ru":
            case "bk.ru":
                smtp = "smtp.mail.ru";
                break;
            case "gmail.com":
                smtp = "smtp.gmail.com";
                break;
            case "yahoo.com":
                smtp = "smtp.mail.yahoo.com";
                break;
            case "yandex.ru":
                smtp = "smtp.yandex.ru";
                break;
            case "rambler.ru":
                smtp = "smtp.rambler.ru";
                break;
        }
        return smtp;
    }
}
