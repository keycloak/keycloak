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
 * One source of AWS credentials — an environment variable pair, a role attached to the compute the
 * server runs on, a profile file.
 * <p>
 * A provider reports "not configured" by returning {@code null}, and reports "configured but broken"
 * by throwing. The distinction is what makes {@link AwsCredentialsProviderChain} usable: a missing
 * environment variable must fall through to the next source, while an unreadable web-identity token
 * file must not be silently swallowed into an unrelated later failure.
 */
public interface AwsCredentialsProvider {

    /**
     * @param transport used by the sources that have to call an HTTP endpoint (STS, the container
     *                  credential endpoint, the instance metadata service); ignored by the others
     * @return the credentials, or {@code null} when this source is not configured in this environment
     * @throws AwsCredentialsException when the source is configured but cannot be used
     */
    AwsCredentials resolve(AwsHttpTransport transport) throws AwsCredentialsException;

    /** Short human-readable name, used in log lines and in the "no credentials" error message. */
    String name();

    /**
     * Whether this source must be reached without a proxy.
     * <p>
     * True for the sources that answer on this machine — the container credential endpoint and the
     * instance metadata service. Their requests carry a bearer token, and a proxy in the path would
     * both receive that token and become the peer the address checks were meant to pin down. The
     * sources that talk to a real AWS endpoint say false and keep the server's outbound configuration.
     */
    default boolean requiresDirectConnection() {
        return false;
    }
}
