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

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMessage.RecipientType;
import jakarta.mail.internet.MimeMultipart;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MailAssert {

    private static final Logger log = Logger.getLogger(MailAssert.class);
    
    public static String assertEmailAndGetUrl(String from, String recipient, String content, Boolean sslEnabled) {

        try {
            MimeMessage message;
            if (sslEnabled){
                message= SslMailServer.getLastReceivedMessage();
            } else {
                message = MailServer.getLastReceivedMessage();
            }            
            assertNotNull("There is no received email.", message);
            assertEquals(recipient, message.getRecipients(RecipientType.TO)[0].toString());
            assertEquals(from, message.getFrom()[0].toString());

            String messageContent;
            if (message.getContent() instanceof MimeMultipart) {
                MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();

                // TEXT content is on index 0
                messageContent = String.valueOf(mimeMultipart.getBodyPart(0).getContent());
            } else {
                messageContent = String.valueOf(message.getContent());
            }
            logMessageContent(messageContent);
            String errorMessage = "Email content should contains \"" + content
                    + "\", but it doesn't.\nEmail content:\n" + messageContent + "\n";

            assertTrue(errorMessage, messageContent.contains(content));
            for (String string : messageContent.split("\n")) {
                if (string.startsWith("http")) {
                    // Ampersand escaped in the text version. Needs to be replaced to have correct URL
                    string = string.replace("&amp;", "&");
                    return string;
                }
            }
            return null;
        } catch (IOException | MessagingException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void logMessageContent(String messageContent) {
        log.debug("---------------------");
        log.debug(messageContent);
        log.debug("---------------------");
    }

}
