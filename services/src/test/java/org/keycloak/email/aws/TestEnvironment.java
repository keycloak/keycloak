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

package org.keycloak.email.aws;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.email.aws.credentials.AwsEnvironment;

/** An {@link AwsEnvironment} backed by plain maps, so tests never touch the real process state. */
public final class TestEnvironment implements AwsEnvironment {

    private final Map<String, String> variables = new HashMap<>();
    private final Map<String, String> properties = new HashMap<>();

    private Path userHome = Paths.get("/nonexistent-home");

    public static TestEnvironment empty() {
        return new TestEnvironment();
    }

    public TestEnvironment with(String name, String value) {
        variables.put(name, value);
        return this;
    }

    public TestEnvironment withProperty(String name, String value) {
        properties.put(name, value);
        return this;
    }

    public TestEnvironment withUserHome(Path home) {
        this.userHome = home;
        return this;
    }

    @Override
    public String getenv(String name) {
        return variables.get(name);
    }

    @Override
    public String getProperty(String name) {
        return properties.get(name);
    }

    @Override
    public Path userHome() {
        return userHome;
    }
}
