/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.utils;

import java.net.IDN;
import java.util.Map;

import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.models.KeycloakSession;

/**
 * SMTP utility methods.
 *
 * @author rmartinc
 */
public class SMTPUtil {

    private SMTPUtil() {
        // static helper class
    }

    /**
     * Validates the configuration using the email sender provider.
     *
     * @param session The keycloak session to use
     * @param config The configuration to validate
     * @throws EmailException If some error is found in the configuration
     */
    public static void checkSMTPConfiguration(KeycloakSession session, Map<String, String> config) throws EmailException {
        if (config == null || config.isEmpty()) {
            return;
        }

        final EmailSenderProvider sender = session.getProvider(EmailSenderProvider.class);
        sender.validate(config);
    }

    /**
     * Converts an email address to its ASCII representation using punycode
     * (IDN.toASCII) for the domain part. The local part is not modified.
     *
     * @param email The email to convert
     * @return The converted email or null (if IDN.toASCII throws an exception)
     */
    public static String convertIDNEmailAddress(String email) {
        final int idx = email == null ? -1 : email.lastIndexOf('@');
        if (idx < 0) {
            return email;
        }
        try {
            return email.substring(0, idx) + '@' + IDN.toASCII(email.substring(idx + 1));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
