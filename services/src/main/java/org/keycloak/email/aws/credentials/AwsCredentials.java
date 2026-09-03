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

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * An AWS access key pair, optionally temporary.
 *
 * @param accessKeyId     the {@code AKIA…}/{@code ASIA…} identifier; the only part of a credential
 *                        that may be logged
 * @param secretAccessKey the signing secret — never log, never put in an exception message
 * @param sessionToken    the STS session token for temporary credentials, {@code null} for a long
 *                        lived IAM user key pair
 * @param expiration      when the credentials stop being valid, {@code null} when they do not expire
 */
public record AwsCredentials(String accessKeyId, String secretAccessKey, String sessionToken, Instant expiration) {

    public AwsCredentials {
        Objects.requireNonNull(accessKeyId, "accessKeyId");
        Objects.requireNonNull(secretAccessKey, "secretAccessKey");
    }

    public static AwsCredentials of(String accessKeyId, String secretAccessKey) {
        return new AwsCredentials(accessKeyId, secretAccessKey, null, null);
    }

    public boolean isTemporary() {
        return sessionToken != null && !sessionToken.isBlank();
    }

    /**
     * Whether these credentials should be replaced. Mirrors what the AWS SDKs do: refresh a few
     * minutes <em>before</em> the stated expiry, so a request signed at the edge of the window is
     * still valid when it reaches AWS.
     */
    public boolean isExpired(Instant now, Duration refreshWindow) {
        return expiration != null && !now.plus(refreshWindow).isBefore(expiration);
    }

    /**
     * Redacted rendering. {@code toString()} on a record prints every component, which would put the
     * secret access key into any log line or exception that happens to interpolate the object.
     */
    @Override
    public String toString() {
        return "AwsCredentials[accessKeyId=" + accessKeyId + ", temporary=" + isTemporary()
                + ", expiration=" + expiration + "]";
    }
}
