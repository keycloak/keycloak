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

package org.keycloak.email;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultEmailSenderProviderFactory implements EmailSenderProviderFactory {

    private final Map<EmailAuthenticator.AuthenticatorType, EmailAuthenticator> emailAuthenticators = new ConcurrentHashMap<>();

    @Override
    public EmailSenderProvider create(KeycloakSession session) {
        return new DefaultEmailSenderProvider(session, emailAuthenticators);
    }

    @Override
    public void init(Config.Scope config) {
        emailAuthenticators.put(EmailAuthenticator.AuthenticatorType.NONE, new DefaultEmailAuthenticator());
        emailAuthenticators.put(EmailAuthenticator.AuthenticatorType.BASIC, new PasswordAuthEmailAuthenticator());
        emailAuthenticators.put(EmailAuthenticator.AuthenticatorType.TOKEN, new TokenAuthEmailAuthenticator());
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
        emailAuthenticators.clear();
    }

    @Override
    public String getId() {
        return "default";
    }

    public Map<EmailAuthenticator.AuthenticatorType, EmailAuthenticator> getEmailAuthenticators() {
        return emailAuthenticators;
    }
}
