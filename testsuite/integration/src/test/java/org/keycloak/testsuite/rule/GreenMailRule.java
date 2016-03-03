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
package org.keycloak.testsuite.rule;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.rules.ExternalResource;
import org.keycloak.models.RealmModel;

import javax.mail.internet.MimeMessage;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class GreenMailRule extends ExternalResource {

    private GreenMail greenMail;

    @Override
    protected void before() throws Throwable {
        ServerSetup setup = new ServerSetup(3025, "localhost", "smtp");

        greenMail = new GreenMail(setup);
        greenMail.start();
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

}
