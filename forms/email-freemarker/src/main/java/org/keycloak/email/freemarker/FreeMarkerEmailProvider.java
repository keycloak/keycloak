package org.keycloak.email.freemarker;

import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailProvider;
import org.keycloak.email.freemarker.beans.EventBean;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.freemarker.FreeMarkerUtil;
import org.keycloak.freemarker.LocaleHelper;
import org.keycloak.freemarker.Theme;
import org.keycloak.freemarker.ThemeProvider;
import org.keycloak.freemarker.beans.MessageFormatterMethod;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.text.MessageFormat;
import java.util.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FreeMarkerEmailProvider implements EmailProvider {

    private static final Logger log = Logger.getLogger(FreeMarkerEmailProvider.class);

    private KeycloakSession session;
    private FreeMarkerUtil freeMarker;
    private RealmModel realm;
    private UserModel user;

    public FreeMarkerEmailProvider(KeycloakSession session, FreeMarkerUtil freeMarker) {
        this.session = session;
        this.freeMarker = freeMarker;
    }

    @Override
    public EmailProvider setRealm(RealmModel realm) {
        this.realm = realm;
        return this;
    }

    @Override
    public EmailProvider setUser(UserModel user) {
        this.user = user;
        return this;
    }

    @Override
    public void sendEvent(Event event) throws EmailException {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("event", new EventBean(event));

        send(toCamelCase(event.getType()) + "Subject", "event-" + event.getType().toString().toLowerCase() + ".ftl", attributes);
    }

    @Override
    public void sendPasswordReset(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("link", link);
        attributes.put("linkExpiration", expirationInMinutes);

        send("passwordResetSubject", "password-reset.ftl", attributes);
    }

    @Override
    public void sendVerifyEmail(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("link", link);
        attributes.put("linkExpiration", expirationInMinutes);

        send("emailVerificationSubject", "email-verification.ftl", attributes);
    }

    private void send(String subjectKey, String template, Map<String, Object> attributes) throws EmailException {
        try {
            ThemeProvider themeProvider = session.getProvider(ThemeProvider.class, "extending");
            Theme theme = themeProvider.getTheme(realm.getEmailTheme(), Theme.Type.EMAIL);
            Locale locale = LocaleHelper.getLocale(realm, user);
            attributes.put("locale", locale);
            Properties rb = theme.getMessages(locale);
            attributes.put("msg", new MessageFormatterMethod(locale, rb));
            String subject = new MessageFormat(rb.getProperty(subjectKey,subjectKey),locale).format(new Object[0]);
            String body = freeMarker.processTemplate(attributes, template, theme);

            send(subject, body);
        } catch (Exception e) {
            throw new EmailException("Failed to template email", e);
        }
    }


    private void send(String subject, String body) throws EmailException {
        try {
            String address = user.getEmail();
            Map<String, String> config = realm.getSmtpConfig();

            Properties props = new Properties();
            props.setProperty("mail.smtp.host", config.get("host"));

            boolean auth = "true".equals(config.get("auth"));
            boolean ssl = "true".equals(config.get("ssl"));
            boolean starttls = "true".equals(config.get("starttls"));

            if (config.containsKey("port")) {
                props.setProperty("mail.smtp.port", config.get("port"));
            }

            if (auth) {
                props.put("mail.smtp.auth", "true");
            }

            if (ssl) {
                props.put("mail.smtp.socketFactory.port", config.get("port"));
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            }

            if (starttls) {
                props.put("mail.smtp.starttls.enable", "true");
            }

            String from = config.get("from");

            Session session = Session.getInstance(props);

            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from));
            msg.setHeader("To", address);
            msg.setSubject(subject);
            msg.setText(body);
            msg.saveChanges();
            msg.setSentDate(new Date());

            Transport transport = session.getTransport("smtp");
            if (auth) {
                transport.connect(config.get("user"), config.get("password"));
            } else {
                transport.connect();
            }
            transport.sendMessage(msg, new InternetAddress[]{new InternetAddress(address)});
        } catch (Exception e) {
            log.warn("Failed to send email", e);
            throw new EmailException(e);
        }
    }

    @Override
    public void close() {
    }

    private String toCamelCase(EventType event){
        StringBuilder sb = new StringBuilder("event");
        for(String s : event.name().toString().toLowerCase().split("_")){
            sb.append(s.substring(0,1).toUpperCase()).append(s.substring(1));
        }
        return sb.toString();
    }

}
