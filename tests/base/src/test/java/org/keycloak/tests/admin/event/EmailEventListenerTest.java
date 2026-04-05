/*
 * Copyright 2023 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.tests.admin.event;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.keycloak.events.email.EmailEventListenerProviderFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class EmailEventListenerTest {

    @InjectRealm(config = EmailSenderRealmConfig.class)
    ManagedRealm realm;

    @InjectUser(config = UserWithEmail.class)
    ManagedUser user;

    @InjectMailServer
    MailServer mail;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @Test
    public void testFailedLoginEmailEvent() throws MessagingException {
        oAuthClient.doPasswordGrantRequest(user.getUsername(), "invalid");

        mail.waitForIncomingEmail(1);
        MimeMessage lastReceivedMessage = mail.getLastReceivedMessage();
        Assertions.assertEquals("Login error", lastReceivedMessage.getSubject());
    }

    public static class EmailSenderRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            return realm.eventsListeners(EmailEventListenerProviderFactory.ID);
        }
    }

    public static class UserWithEmail implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder user) {
            return user.username("test").email("test@local").password("password").emailVerified(true);
        }
    }

}
