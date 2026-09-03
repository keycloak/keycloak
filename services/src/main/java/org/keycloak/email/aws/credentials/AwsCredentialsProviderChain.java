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
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.keycloak.email.aws.AwsHttpTransport;

import org.jboss.logging.Logger;

/**
 * Resolves credentials from the first configured source, in the order the AWS SDK for Java v2 uses:
 * JVM system properties, environment variables, web identity token (EKS IRSA), profile files,
 * container credentials (ECS / EKS Pod Identity), then the EC2 instance metadata service.
 * <p>
 * The order is not arbitrary and is not the place to be creative: an operator who exports
 * {@code AWS_ACCESS_KEY_ID} on a machine that also has {@code ~/.aws/credentials} expects the
 * environment to win, exactly as every other AWS tool on that machine behaves.
 * <p>
 * Resolved credentials are cached until shortly before they expire. Long-lived IAM user keys never
 * expire and are resolved once; the role-based sources return short-lived credentials and are
 * refreshed {@value #REFRESH_WINDOW_MINUTES} minutes ahead of expiry, so a message signed at the
 * edge of the window is still valid when it reaches AWS.
 */
public final class AwsCredentialsProviderChain {

    static final int REFRESH_WINDOW_MINUTES = 5;

    private static final Logger logger = Logger.getLogger(AwsCredentialsProviderChain.class);
    private static final Duration REFRESH_WINDOW = Duration.ofMinutes(REFRESH_WINDOW_MINUTES);

    private final List<AwsCredentialsProvider> providers;
    private final Object refreshLock = new Object();

    private volatile AwsCredentials cached;

    public AwsCredentialsProviderChain(List<AwsCredentialsProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    /**
     * The default chain. {@code environment} is injected rather than read from {@link System} so the
     * whole chain is exercisable from a test without mutating the JVM's real environment.
     */
    public static AwsCredentialsProviderChain defaultChain(AwsEnvironment environment) {
        List<AwsCredentialsProvider> providers = new ArrayList<>();
        providers.add(new SystemPropertiesCredentialsProvider(environment));
        providers.add(new EnvironmentVariableCredentialsProvider(environment));
        providers.add(new WebIdentityTokenCredentialsProvider(environment));
        providers.add(new ProfileCredentialsProvider(environment));
        providers.add(new ContainerCredentialsProvider(environment));
        providers.add(new InstanceMetadataCredentialsProvider(environment));
        return new AwsCredentialsProviderChain(providers);
    }

    /**
     * @param transport       the server's own HTTP client, used for the sources that talk to AWS
     * @param directTransport a proxy-free client, used for the sources that answer on this machine
     */
    public AwsCredentials resolve(AwsHttpTransport transport, AwsHttpTransport directTransport, Instant now)
            throws AwsCredentialsException {
        AwsCredentials current = cached;
        if (current != null && !current.isExpired(now, REFRESH_WINDOW)) {
            return current;
        }
        synchronized (refreshLock) {
            // Re-read under the lock: while this thread waited, another one may have refreshed. The
            // sources that cost an HTTP round trip are the ones that expire, so letting every
            // concurrent send re-fetch would multiply calls to the metadata service under load.
            current = cached;
            if (current != null && !current.isExpired(now, REFRESH_WINDOW)) {
                return current;
            }
            AwsCredentials resolved = resolveFromSources(transport, directTransport);
            // Temporary credentials with no stated expiry are not cached. They come from a profile
            // file or the process environment, where a session token can be pasted in by hand; once
            // it expires there is nothing here that could notice, and caching it would keep every
            // email broken even after an operator refreshed the file.
            cached = resolved.isTemporary() && resolved.expiration() == null ? null : resolved;
            return resolved;
        }
    }

    private AwsCredentials resolveFromSources(AwsHttpTransport transport, AwsHttpTransport directTransport)
            throws AwsCredentialsException {
        StringJoiner attempted = new StringJoiner(", ");
        for (AwsCredentialsProvider provider : providers) {
            attempted.add(provider.name());
            AwsCredentials credentials =
                    provider.resolve(provider.requiresDirectConnection() ? directTransport : transport);
            if (credentials != null) {
                logger.debugf("Resolved AWS credentials from %s (access key id %s)", provider.name(),
                        credentials.accessKeyId());
                return credentials;
            }
        }
        throw new AwsCredentialsException("No AWS credentials found. Sources tried, in order: " + attempted
                + ". Set AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY in the Keycloak environment, or run the"
                + " server with an IAM role attached.");
    }

    /** Visible for testing: drops the cache so the next resolve re-runs the chain. */
    void invalidate() {
        cached = null;
    }
}
