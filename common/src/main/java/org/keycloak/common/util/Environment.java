/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.common.util;

import java.security.Provider;
import java.security.Security;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class Environment {

    public static final boolean IS_IBM_JAVA = System.getProperty("java.vendor").contains("IBM");

    public static final int DEFAULT_JBOSS_AS_STARTUP_TIMEOUT = 300;

    public static final String PROFILE = "kc.profile";
    public static final String ENV_PROFILE = "KC_PROFILE";
    public static final String DEV_PROFILE_VALUE = "dev";

    public static int getServerStartupTimeout() {
        String timeout = System.getProperty("jboss.as.management.blocking.timeout");
        if (timeout != null) {
            return Integer.parseInt(timeout);
        } else {
            return DEFAULT_JBOSS_AS_STARTUP_TIMEOUT;
        }
    }

    /**
     * Tries to detect if Java platform is in the FIPS mode
     * @return true if java is FIPS mode
     */
    public static boolean isJavaInFipsMode() {
        // Check if FIPS explicitly enabled by system property
        String property = System.getProperty("com.redhat.fips");
        if (property != null) {
            return Boolean.parseBoolean(property);
        }

        // Otherwise try to auto-detect
        for (Provider provider : Security.getProviders()) {
            if (provider.getName().equals("BCFIPS")) continue; // Ignore BCFIPS provider for the detection as we may register it programatically
            if (provider.getName().toUpperCase().contains("FIPS")) return true;
        }
        return false;
    }

    public static boolean isDevMode() {
        return DEV_PROFILE_VALUE.equalsIgnoreCase(getProfile());
    }

    public static String getProfile() {
        String profile = System.getProperty(PROFILE);

        if (profile != null) {
            return profile;
        }

        return System.getenv(ENV_PROFILE);
    }
}
