/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

import picocli.CommandLine;

public final class Messages {

    private Messages() {

    }

    public static IllegalArgumentException invalidDatabaseVendor(String db, String... availableOptions) {
        return new IllegalArgumentException("Invalid database vendor [" + db + "]. Possible values are: " + String.join(", ", availableOptions) + ".");
    }

    public static IllegalArgumentException invalidProxyMode(String mode) {
        return new IllegalArgumentException("Invalid value [" + mode + "] for configuration property [proxy].");
    }

    public static IllegalStateException httpsConfigurationNotSet() {
        StringBuilder builder = new StringBuilder("Key material not provided to setup HTTPS. Please configure your keys/certificates");
        if (!Environment.DEV_PROFILE_VALUE.equals(Environment.getProfile())) {
            builder.append(" or start the server in development mode");
        }
        builder.append(".");
        return new IllegalStateException(builder.toString());
    }

    public static void cliExecutionError(CommandLine cmd, String message, Throwable cause) {
        throw new CommandLine.ExecutionException(cmd, message, cause);
    }

    public static String devProfileNotAllowedError(String cmd) {
        return String.format("You can not '%s' the server in %s mode. Please re-build the server first, using 'kc.sh build' for the default production mode.%n", cmd, Environment.getKeycloakModeFromProfile(Environment.DEV_PROFILE_VALUE));
    }

    public static Throwable invalidLogLevel(String logLevel) {
        Set<String> values = Arrays.stream(Logger.Level.values()).map(Logger.Level::name).map(String::toLowerCase).collect(Collectors.toSet());
        return new IllegalStateException("Invalid log level: " + logLevel + ". Possible values are: " + String.join(", ", values) + ".");
    }

    public static Throwable invalidLogCategoryFormat(String category) {
        return new IllegalStateException("Invalid log category format: " + category + ". The format is 'category:level' such as 'org.keycloak:debug'.");
    }
}
