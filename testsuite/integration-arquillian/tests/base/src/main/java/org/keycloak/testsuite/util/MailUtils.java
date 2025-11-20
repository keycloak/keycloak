/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.util;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MailUtils {

    private static Pattern mailPattern = Pattern.compile("http[^\\s\"]*");

    public static String getLink(String body) {
        Matcher matcher = mailPattern.matcher(body);
        if (matcher.find()) {
            return matcher.group();
        }
        throw new AssertionError("No link found in " + body);
    }

    public static String getPasswordResetEmailLink(MimeMessage message) throws IOException {
        return getPasswordResetEmailLink(new EmailBody(message));
    }

    /**
     *
     * @param message email message
     * @return first recipient of the email message
     * @throws MessagingException
     */
    public static String getRecipient(MimeMessage message) throws MessagingException {
        Address[] recipients = message.getRecipients(MimeMessage.RecipientType.TO);
        return recipients[0].toString();
    }

    public static String getPasswordResetEmailLink(EmailBody body) throws IOException {
        final String textChangePwdUrl = getLink(body.getText());
        String htmlChangePwdUrl = getLink(body.getHtml());
        
        assertEquals(htmlChangePwdUrl, textChangePwdUrl);

        return htmlChangePwdUrl;
    }

    public static EmailBody getBody(MimeMessage message) throws IOException {
        return new EmailBody(message);
    }

    public static class EmailBody {

        private String text;
        private String html;

        private EmailBody(MimeMessage message) throws IOException {
            try {
                Multipart multipart = (Multipart) message.getContent();

                String textContentType = multipart.getBodyPart(0).getContentType();

                assertEquals("text/plain; charset=UTF-8", textContentType);

                text = (String) multipart.getBodyPart(0).getContent();

                String htmlContentType = multipart.getBodyPart(1).getContentType();

                assertEquals("text/html; charset=UTF-8", htmlContentType);

                html = (String) multipart.getBodyPart(1).getContent();
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        }

        public String getText() {
            return text;
        }

        public String getHtml() {
            return html;
        }
    }

}
