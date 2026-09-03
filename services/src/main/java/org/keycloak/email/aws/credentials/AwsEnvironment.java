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

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The ambient process state the AWS credential chain reads: environment variables, JVM system
 * properties and the user's home directory.
 * <p>
 * It exists as an interface for one reason: {@link System#getenv(String)} cannot be set from a test,
 * so without this indirection the environment-variable, container and instance-profile branches of
 * the chain would only be exercisable by mutating the JVM's real environment. Production code uses
 * {@link #SYSTEM}.
 */
public interface AwsEnvironment {

    AwsEnvironment SYSTEM = new AwsEnvironment() {

        @Override
        public String getenv(String name) {
            return System.getenv(name);
        }

        @Override
        public String getProperty(String name) {
            return System.getProperty(name);
        }

        @Override
        public Path userHome() {
            return Paths.get(System.getProperty("user.home", ""));
        }
    };

    String getenv(String name);

    String getProperty(String name);

    Path userHome();

    /**
     * Reads {@code name} and returns {@code null} when it is unset <em>or blank</em>.
     * <p>
     * Blank has to collapse into unset: docker compose's {@code KEY: ${VAR:-}} form injects an empty
     * string rather than omitting the variable, so a chain link that only checked for {@code null}
     * would accept empty credentials and fail later with an opaque SES {@code SignatureDoesNotMatch}
     * instead of falling through to the next link.
     */
    default String value(String name) {
        String value = getenv(name);
        return value == null || value.isBlank() ? null : value;
    }

    /** {@link #value(String)} for a JVM system property. */
    default String property(String name) {
        String value = getProperty(name);
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * A setting the AWS SDKs expose both ways, read in their order: the JVM system property wins over
     * the environment variable.
     * <p>
     * Reading only the variable would make this provider disagree with every other AWS client in the
     * same JVM — a server started with {@code -Daws.profile=…} would send email as one identity while
     * everything else used another.
     */
    default String setting(String systemProperty, String environmentVariable) {
        String property = property(systemProperty);
        return property != null ? property : value(environmentVariable);
    }
}
