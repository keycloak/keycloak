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
 * Credentials from {@code AWS_ACCESS_KEY_ID}, {@code AWS_SECRET_ACCESS_KEY} and the optional
 * {@code AWS_SESSION_TOKEN} — the form docker compose files, systemd units and Kubernetes secrets
 * all speak, and the one an operator reaches for first.
 * <p>
 * The source is process-local: it never uses the transport and cannot block. Variables are read
 * through {@link AwsEnvironment#value(String)}, so a variable that exists but is empty counts as
 * unset and the chain moves on — the difference between falling through to the instance role and
 * signing every message with an empty secret.
 * <p>
 * A pair found here is reported as non-expiring even when it carries a session token: the process
 * environment is fixed at exec time, so re-resolving would only read the same value back.
 */
public final class EnvironmentVariableCredentialsProvider implements AwsCredentialsProvider {

    private static final String ACCESS_KEY_ID = "AWS_ACCESS_KEY_ID";
    private static final String SECRET_ACCESS_KEY = "AWS_SECRET_ACCESS_KEY";
    private static final String SESSION_TOKEN = "AWS_SESSION_TOKEN";

    private final AwsEnvironment environment;

    public EnvironmentVariableCredentialsProvider(AwsEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public AwsCredentials resolve(AwsHttpTransport transport) throws AwsCredentialsException {
        String accessKeyId = environment.value(ACCESS_KEY_ID);
        String secretAccessKey = environment.value(SECRET_ACCESS_KEY);
        if (accessKeyId == null && secretAccessKey == null) {
            return null;
        }
        // Half of a key pair is a typo, not an unconfigured source: falling through would hide the
        // mistake behind whatever the next source happens to find, or behind "no credentials found".
        if (accessKeyId == null) {
            throw new AwsCredentialsException("Environment variable " + SECRET_ACCESS_KEY + " is set but "
                    + ACCESS_KEY_ID + " is not. Set both, or neither.");
        }
        if (secretAccessKey == null) {
            throw new AwsCredentialsException("Environment variable " + ACCESS_KEY_ID + " is set but "
                    + SECRET_ACCESS_KEY + " is not. Set both, or neither.");
        }
        return new AwsCredentials(accessKeyId, secretAccessKey, environment.value(SESSION_TOKEN), null);
    }

    @Override
    public String name() {
        return "environment variables";
    }
}
