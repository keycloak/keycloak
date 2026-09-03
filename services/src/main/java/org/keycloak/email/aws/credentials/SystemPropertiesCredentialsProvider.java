/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.email.aws.credentials;

import org.keycloak.email.aws.AwsHttpTransport;

/**
 * Credentials from the JVM system properties {@code aws.accessKeyId}, {@code aws.secretAccessKey}
 * and the optional {@code aws.sessionToken}, the same three properties every AWS SDK reads.
 * <p>
 * The source is process-local: it never uses the transport and cannot block, which is also why it
 * is cheap enough to sit first in the chain. It is the only source an operator can set per server
 * process — through {@code JAVA_OPTS_APPEND} — without touching the environment of the whole
 * machine, so it has to win over the variables that machine may already export for other tools.
 * <p>
 * A pair found here is reported as non-expiring even when it carries a session token: nothing
 * rewrites a system property while the JVM runs, so re-resolving would only read the same value
 * back, and an expiry would buy an endless refresh loop instead of a refreshed credential.
 */
public final class SystemPropertiesCredentialsProvider implements AwsCredentialsProvider {

    private static final String ACCESS_KEY_ID = "aws.accessKeyId";
    private static final String SECRET_ACCESS_KEY = "aws.secretAccessKey";
    private static final String SESSION_TOKEN = "aws.sessionToken";

    private final AwsEnvironment environment;

    public SystemPropertiesCredentialsProvider(AwsEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public AwsCredentials resolve(AwsHttpTransport transport) throws AwsCredentialsException {
        String accessKeyId = environment.property(ACCESS_KEY_ID);
        String secretAccessKey = environment.property(SECRET_ACCESS_KEY);
        if (accessKeyId == null && secretAccessKey == null) {
            return null;
        }
        // Half of a key pair is a typo, not an unconfigured source: falling through would hide the
        // mistake behind whatever the next source happens to find, or behind "no credentials found".
        if (accessKeyId == null) {
            throw new AwsCredentialsException("System property " + SECRET_ACCESS_KEY + " is set but "
                    + ACCESS_KEY_ID + " is not. Set both, or neither.");
        }
        if (secretAccessKey == null) {
            throw new AwsCredentialsException("System property " + ACCESS_KEY_ID + " is set but "
                    + SECRET_ACCESS_KEY + " is not. Set both, or neither.");
        }
        return new AwsCredentials(accessKeyId, secretAccessKey, environment.property(SESSION_TOKEN), null);
    }

    @Override
    public String name() {
        return "JVM system properties";
    }
}
