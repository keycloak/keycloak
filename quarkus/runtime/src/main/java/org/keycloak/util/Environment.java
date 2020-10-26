/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.util;

import java.util.Optional;

import org.apache.commons.lang3.SystemUtils;
import org.keycloak.configuration.Configuration;

public final class Environment {

    public static Boolean isRebuild() {
        return Boolean.valueOf(System.getProperty("quarkus.launch.rebuild"));
    }

    public static String getHomeDir() {
        return System.getProperty("kc.home.dir");
    }

    public static String getCommand() {
        String homeDir = getHomeDir();

        if (homeDir == null) {
            return "java -jar $KEYCLOAK_HOME/lib/quarkus-run.jar";
        }

        if (isWindows()) {
            return "kc.bat";
        }
        return "kc.sh";
    }
    
    public static String getConfigArgs() {
        return System.getProperty("kc.config.args");
    }

    public static String getProfile() {
        String profile = System.getProperty("kc.profile");
        
        if (profile == null) {
            profile = System.getenv("KC_PROFILE");
        }

        return profile;
    }

    public static String getProfileOrDefault(String defaultProfile) {
        String profile = getProfile();

        if (profile == null) {
            profile = defaultProfile;
        }
        
        return profile;
    }

    public static Optional<String> getBuiltTimeProperty(String name) {
        String value = Configuration.getBuiltTimeProperty(name);

        if (value == null) {
            return Optional.empty();
        }
        
        return Optional.of(value);
    }

    public static boolean isDevMode() {
        return "dev".equalsIgnoreCase(getProfile());
    }

    public static boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }
}
