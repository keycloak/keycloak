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

import static org.keycloak.quarkus.runtime.configuration.Configuration.getBuildTimeProperty;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ProfileManager;
import org.apache.commons.lang3.SystemUtils;

public final class Environment {

    public static final String IMPORT_EXPORT_MODE = "import_export";
    public static final String PROFILE ="kc.profile";
    public static final String ENV_PROFILE ="KC_PROFILE";
    public static final String DATA_PATH = "/data";
    public static final String DEFAULT_THEMES_PATH = "/themes";
    public static final String DEV_PROFILE_VALUE = "dev";
    public static final String LAUNCH_MODE = "kc.launch.mode";

    private Environment() {}

    public static Boolean isRebuild() {
        return Boolean.getBoolean("quarkus.launch.rebuild");
    }

    public static String getHomeDir() {
        return System.getProperty("kc.home.dir");
    }

    public static Path getHomePath() {
        String homeDir = getHomeDir();

        if (homeDir != null) {
            return Paths.get(homeDir);
        }

        return null;
    }

    public static String getDataDir() {
        return getHomeDir() + DATA_PATH;
    }

    public static String getDefaultThemeRootDir() {
        return getHomeDir() + DEFAULT_THEMES_PATH;
    }

    public static Path getProvidersPath() {
        Path homePath = Environment.getHomePath();

        if (homePath != null) {
            return homePath.resolve("providers");
        }

        return null;
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

    public static String getProfile() {
        String profile = System.getProperty(PROFILE);
        
        if (profile == null) {
            profile = System.getenv(ENV_PROFILE);
        }

        return profile;
    }

    public static void setProfile(String profile) {
        System.setProperty(PROFILE, profile);
        System.setProperty("quarkus.profile", profile);
        if (isTestLaunchMode()) {
            System.setProperty("mp.config.profile", profile);
            System.setProperty(ProfileManager.QUARKUS_TEST_PROFILE_PROP, profile);
        }
    }

    public static String getProfileOrDefault(String defaultProfile) {
        String profile = getProfile();

        if (profile == null) {
            profile = defaultProfile;
        }
        
        return profile;
    }

    public static boolean isDevMode() {
        if (DEV_PROFILE_VALUE.equalsIgnoreCase(getProfile())) {
            return true;
        }

        return DEV_PROFILE_VALUE.equals(getBuildTimeProperty(PROFILE).orElse(null));
    }

    public static boolean isDevProfile(){
        return Optional.ofNullable(getProfile()).orElse("").equalsIgnoreCase(DEV_PROFILE_VALUE);
    }

    public static boolean isImportExportMode() {
        return IMPORT_EXPORT_MODE.equalsIgnoreCase(getProfile());
    }

    public static boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    public static void forceDevProfile() {
        setProfile(DEV_PROFILE_VALUE);
    }

    public static Map<String, File> getProviderFiles() {
        Path providersPath = Environment.getProvidersPath();

        if (providersPath == null) {
            return Collections.emptyMap();
        }

        File providersDir = providersPath.toFile();

        if (!providersDir.exists() || !providersDir.isDirectory()) {
            throw new RuntimeException("The 'providers' directory does not exist or is not a valid directory.");
        }

        return Arrays.stream(providersDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        })).collect(Collectors.toMap(File::getName, Function.identity()));
    }

    public static boolean isQuarkusDevMode() {
        return ProfileManager.getLaunchMode().equals(LaunchMode.DEVELOPMENT);
    }

    public static boolean isTestLaunchMode() {
        return "test".equals(System.getProperty(LAUNCH_MODE));
    }

    public static void forceTestLaunchMode() {
        System.setProperty(LAUNCH_MODE, "test");
    }

    public static boolean isDistribution() {
        return Environment.getCommand().startsWith("kc.");
    }
}
