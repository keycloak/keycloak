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

import java.lang.Thread.UncaughtExceptionHandler;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import jakarta.mail.internet.MimeMessage;

import org.keycloak.models.RealmModel;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.rules.ExternalResource;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class GreenMailRule extends ExternalResource {

    private GreenMail greenMail;

    private int port = 3025;
    private String host = "localhost";

    public GreenMailRule() {
    }

    public GreenMailRule(int port, String host) {
        this.port = port;
        this.host = host;
    }

    @Override
    protected void before() throws Throwable {
        ServerSetup setup = new ServerSetup(port, host, "smtp");

        greenMail = new GreenMail(setup);
        greenMail.start();
    }

    public void credentials(String username, String password) {
        greenMail.setUser(username, password);
    }

    @Override
    protected void after() {
        if (greenMail != null) {
            // Suppress error from GreenMail on shutdown
            Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    if (!(e.getCause() instanceof SocketException && t.getClass().getName()
                            .equals("com.icegreen.greenmail.smtp.SmtpHandler"))) {
                        System.err.print("Exception in thread \"" + t.getName() + "\" ");
                        e.printStackTrace(System.err);
                    }
                }
            });

            greenMail.stop();
        }
    }

    public void configureRealm(RealmModel realm) {
        Map<String, String> config = new HashMap<>();
        config.put("from", "auto@keycloak.org");
        config.put("host", "localhost");
        config.put("port", "3025");
        realm.setSmtpConfig(config);
    }

    public MimeMessage[] getReceivedMessages() {
        return greenMail.getReceivedMessages();
    }

    /**
     * Returns the very last received message. When no message is available, returns {@code null}.
     * @return see description
     */
    public MimeMessage getLastReceivedMessage() {
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        return (receivedMessages == null || receivedMessages.length == 0)
          ? null
          : receivedMessages[receivedMessages.length - 1];
    }

    /**
     * Use this method if you are sending email in a different thread from the one you're testing from.
     * Block waits for an email to arrive in any mailbox for any user.
     * Implementation Detail: No polling wait implementation
     *
     * @param timeout maximum time in ms to wait for emailCount of messages to arrive before giving up and returning false
     * @param emailCount waits for these many emails to arrive before returning
     * @return
     * @throws InterruptedException
     */
    public boolean waitForIncomingEmail(long timeout, int emailCount) throws InterruptedException {
        return greenMail.waitForIncomingEmail(timeout, emailCount);
    }

    /**
     * Does the same thing as Object.wait(long, int) but with a timeout of 5000ms.
     * @param emailCount waits for these many emails to arrive before returning
     * @return
     * @throws InterruptedException
     */
    public boolean waitForIncomingEmail(int emailCount) throws InterruptedException {
        return greenMail.waitForIncomingEmail(emailCount);
    }
}
