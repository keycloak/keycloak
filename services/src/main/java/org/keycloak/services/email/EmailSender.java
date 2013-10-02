/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.services.email;

import java.net.URI;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.services.managers.AccessCodeEntry;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.AccountService;
import org.keycloak.services.resources.flows.Urls;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class EmailSender {

    private static final Logger log = Logger.getLogger(EmailSender.class);

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

    public void sendEmailVerification(UserModel user, RealmModel realm, AccessCodeEntry accessCode, UriInfo uriInfo) {
        UriBuilder builder = Urls.accountBase(uriInfo.getBaseUri()).path(AccountService.class, "emailVerification");
        builder.queryParam("key", accessCode.getId());

        URI uri = builder.build(realm.getId());

        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(user.getFirstName()).append(",\n\n");
        sb.append("Someone has created a Keycloak account with this email address. ");
        sb.append("If this was you, click the link below to verify your email address:\n");
        sb.append(uri.toString());
        sb.append("\n\nThis link will expire within ").append(TimeUnit.SECONDS.toMinutes(realm.getAccessCodeLifespanUserAction()));
        sb.append(" minutes.\n\n");
        sb.append("If you didn't create this account, just ignore this message.\n\n");
        sb.append("Thanks,\n");
        sb.append("The Keycloak Team");

        try {
            send(user.getEmail(), "Verify email", sb.toString());
        } catch (Exception e1) {
            log.warn("Failed to send email verification");
        }
    }

    public void sendPasswordReset(UserModel user, RealmModel realm, AccessCodeEntry accessCode, UriInfo uriInfo) {
        UriBuilder builder = Urls.accountBase(uriInfo.getBaseUri()).path(AccountService.class, "passwordPage");
        builder.queryParam("key", accessCode.getId());

        URI uri = builder.build(realm.getId());

        StringBuilder sb = new StringBuilder();

        sb.append("Hi ").append(user.getFirstName()).append(",\n\n");
        sb.append("Someone just requested to change your Keycloak account's password. ");
        sb.append("If this was you, click on the link below to set a new password:\n");
        sb.append(uri.toString());
        sb.append("\n\nThis link will expire within ").append(TimeUnit.SECONDS.toMinutes(realm.getAccessCodeLifespanUserAction()));
        sb.append(" minutes.\n\n");
        sb.append("If you don't want to reset your password, just ignore this message and nothing will be changed.\n\n");
        sb.append("Thanks,\n");
        sb.append("The Keycloak Team");

        try {
            send(user.getEmail(), "Reset password link", sb.toString());
        } catch (Exception e) {
            log.warn("Failed to send reset password link", e);
        }
    }

}
