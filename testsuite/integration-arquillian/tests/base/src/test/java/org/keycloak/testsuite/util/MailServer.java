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
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.SocketException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMessage.RecipientType;
import jakarta.mail.internet.MimeMultipart;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.jboss.logging.Logger;

import static org.keycloak.testsuite.util.MailServerConfiguration.HOST;
import static org.keycloak.testsuite.util.MailServerConfiguration.PORT;

public class MailServer {

    private static final Logger log = Logger.getLogger(MailServer.class);
    
    private static GreenMail greenMail;

    public static void main(String[] args) throws Exception {
        MailServer.start();
        MailServer.createEmailAccount("test@email.test", "password");
        
        try {
            while (true) {
                int c = greenMail.getReceivedMessages().length;

                if (greenMail.waitForIncomingEmail(Long.MAX_VALUE, c + 1)) {
                    MimeMessage m = greenMail.getReceivedMessages()[c++];
                    log.info("-------------------------------------------------------");
                    log.info("Received mail to " + m.getRecipients(RecipientType.TO)[0]);
                    if (m.getContent() instanceof MimeMultipart) {
                        MimeMultipart mimeMultipart = (MimeMultipart) m.getContent();
                        for (int i = 0; i < mimeMultipart.getCount(); i++) {
                            log.info("----");
                            log.info(mimeMultipart.getBodyPart(i).getContentType() + ":\n");
                            log.info(mimeMultipart.getBodyPart(i).getContent());
                        }
                    } else {
                        log.info("\n" + m.getContent());
                    }
                    log.info("-------------------------------------------------------");
                }
            }
        } catch (IOException | MessagingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void start() {
        ServerSetup setup = new ServerSetup(Integer.parseInt(PORT), HOST, "smtp");

        greenMail = new GreenMail(setup);
        greenMail.start();

        log.info("Started mail server (" + HOST + ":" + PORT + ")");
    }

    public static void stop() {
        if (greenMail != null) {
            log.info("Stopping mail server (localhost:3025)");
            // Suppress error from GreenMail on shutdown
            Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    if (!(e.getCause() instanceof SocketException && e.getStackTrace()[0].getClassName()
                            .equals("com.icegreen.greenmail.smtp.SmtpHandler"))) {
                        log.error("Exception in thread \"" + t.getName() + "\" ");
                        log.error(e.getMessage(), e);
                    }
                }
            });
            greenMail.stop();
        }
    }

    public static void createEmailAccount(String email, String password) {
        log.debug("Creating email account " + email);
        greenMail.setUser(email, password);
    }
    
    public static MimeMessage getLastReceivedMessage() throws InterruptedException {
        if (greenMail.waitForIncomingEmail(1)) {
            return greenMail.getReceivedMessages()[greenMail.getReceivedMessages().length - 1];
        }
        return null;
    }

    public static MimeMessage[] getReceivedMessages() {
        return greenMail.getReceivedMessages();
    }
}
