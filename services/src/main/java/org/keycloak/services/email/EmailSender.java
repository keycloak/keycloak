package org.keycloak.services.email;

import java.util.Map.Entry;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSender {

    private Properties properties;

    public EmailSender() {
        properties = new Properties();
        for (Entry<Object, Object> e : System.getProperties().entrySet()) {
            String key = (String) e.getKey();
            if (key.startsWith("keycloak.mail.smtp.")) {
                key = key.replace("keycloak.mail.smtp.", "mail.smtp.");
                properties.put(key, e.getValue());
            }
        }
    }

    public void send(String address, String subject, String body) throws AddressException, MessagingException {

        Session session = Session.getDefaultInstance(properties);

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(properties.getProperty("mail.smtp.from")));
        msg.setSubject(subject);
        msg.setText(body);
        msg.saveChanges();

        Transport transport = session.getTransport("smtp");
        transport.connect(properties.getProperty("mail.smtp.user"), properties.getProperty("mail.smtp.password"));
        transport.sendMessage(msg, new InternetAddress[] { new InternetAddress(address) });
    }

}
