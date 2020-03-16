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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;

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

    public static String getPasswordResetEmailLink(EmailBody body) throws IOException {
        final String textChangePwdUrl = getLink(body.getText());
        String htmlChangePwdUrl = getLink(body.getHtml());
        
        // undo changes that may have been made by html sanitizer
        htmlChangePwdUrl = htmlChangePwdUrl.replace("&#61;", "=");
        htmlChangePwdUrl = htmlChangePwdUrl.replace("..", ".");
        htmlChangePwdUrl = htmlChangePwdUrl.replace("&amp;", "&");
        
        assertEquals(htmlChangePwdUrl, textChangePwdUrl);

        return htmlChangePwdUrl;
    }

    public static EmailBody getBody(MimeMessage message) throws IOException {
        return new EmailBody(message);
    }

    public static class EmailBody {

        private String text;
        private String html;
        private Map<String, Object> embedded;

        private EmailBody(MimeMessage message) throws IOException {
            try {
                Multipart multipart = (Multipart) message.getContent();

                multipart.writeTo(System.out);

                String textContentType = multipart.getBodyPart(0).getContentType();

                assertEquals("text/plain; charset=UTF-8", textContentType);

                text = (String) multipart.getBodyPart(0).getContent();

                String contentTypePart2 = multipart.getBodyPart(1).getContentType();
                if (contentTypePart2 != null && contentTypePart2.startsWith("multipart/related")) {
                    Multipart related = (Multipart) multipart.getBodyPart(1).getContent();
                    
                    assertEquals("text/html; charset=UTF-8", related.getBodyPart(0).getContentType());
                    
                    html = (String) related.getBodyPart(0).getContent();

                    embedded = new HashMap<>();
                    for (int i = 1; i < related.getCount(); i++) {
                        assertEquals("image/png", related.getBodyPart(i).getContentType());
                        assertNotNull(related.getBodyPart(i).getHeader("Content-ID"));
                        assertEquals(1, related.getBodyPart(i).getHeader("Content-ID").length);
                        embedded.put(related.getBodyPart(i).getHeader("Content-ID")[0],    related.getBodyPart(i).getContent());
                    }
                } else {
                    assertEquals("text/html; charset=UTF-8", contentTypePart2);
    
                    html = (String) multipart.getBodyPart(1).getContent();
                }
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

        public Map<String, Object> getEmbedded() {
            return embedded;
        }
    }

}
