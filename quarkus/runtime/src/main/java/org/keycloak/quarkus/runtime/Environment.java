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

import org.keycloak.common.Profile;
import org.keycloak.common.util.NetworkUtils;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import io.quarkus.runtime.LaunchMode;
import io.smallrye.config.SmallRyeConfig;

public final class Environment {

    public static final String KC_CONFIG_REBUILD_CHECK = "kc.config.rebuild-check";
    public static final String KC_CONFIG_BUILT = "kc.config.built";
    private static final String KC_HOME_DIR = "kc.home.dir";
    public static final String NON_SERVER_MODE = "nonserver";
    public static final String PROFILE ="kc.profile";
    public static final String ENV_PROFILE ="KC_PROFILE";
    public static final String DATA_PATH = File.separator + "data";
    public static final String DEFAULT_THEMES_PATH = File.separator +  "themes";
    public static final String PROD_PROFILE_VALUE = "prod";
    public static final String LAUNCH_MODE = "kc.launch.mode";

    private Environment() {}

    public static Boolean isRebuild() {
        return Boolean.getBoolean("quarkus.launch.rebuild");
    }

    public static Optional<String> getHomeDir() {
        return Optional.ofNullable(System.getProperty(KC_HOME_DIR));
    }

    public static Optional<Path> getHomePath() {
        return getHomeDir().map(Paths::get);
    }

    public static Optional<String> getDataDir() {
        return getHomeDir().map(p -> p.concat(DATA_PATH));
    }

    public static Optional<String> getDefaultThemeRootDir() {
        return getHomeDir().map(p -> p.concat(DEFAULT_THEMES_PATH));
    }

    public static Optional<Path> getProvidersPath() {
        return Environment.getHomePath().map(p -> p.resolve("providers"));
    }

    public static String getCommand() {
        if (isWindows()) {
            return "kc.bat";
        }

        return "kc.sh";
    }

    public static void setProfile(String profile) {
        System.setProperty(org.keycloak.common.util.Environment.PROFILE, profile);
        System.setProperty(LaunchMode.current().getProfileKey(), profile);
        System.setProperty(SmallRyeConfig.SMALLRYE_CONFIG_PROFILE, profile);
        if (isTestLaunchMode()) {
            System.setProperty("mp.config.profile", profile);
        }
    }

    public static boolean isDevMode() {
        if (org.keycloak.common.util.Environment.isDevMode()) {
            return true;
        }

        return org.keycloak.common.util.Environment.DEV_PROFILE_VALUE.equals(Configuration.getNonPersistedConfigValue(org.keycloak.common.util.Environment.PROFILE).getValue());
    }

    public static boolean isDevProfile(){
        return Optional.ofNullable(org.keycloak.common.util.Environment.getProfile()).orElse("").equalsIgnoreCase(org.keycloak.common.util.Environment.DEV_PROFILE_VALUE);
    }

    public static boolean isNonServerMode() {
        return NON_SERVER_MODE.equalsIgnoreCase(org.keycloak.common.util.Environment.getProfile());
    }

    public static boolean isWindows() {
        return NetworkUtils.checkForWindows();
    }

    public static void forceDevProfile() {
        setProfile(org.keycloak.common.util.Environment.DEV_PROFILE_VALUE);
    }

    public static Map<String, File> getProviderFiles() {
        Path providersPath = Environment.getProvidersPath().orElse(null);

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

    public static boolean isTestLaunchMode() {
        return "test".equals(System.getProperty(LAUNCH_MODE));
    }

    public static void forceTestLaunchMode() {
        System.setProperty(LAUNCH_MODE, "test");
    }

    /**
     * We want to hide the "profiles" from Quarkus to not make things unnecessarily complicated for users,
     * so this method returns the equivalent launch mode instead. For use in e.g. CLI Output.
     *
     * @param profile the internal profile string used
     * @return the mapped launch mode, none when nothing is given or the profile as is when its
     * neither null/empty nor matching the quarkus default profiles we use.
     */
    public static String getKeycloakModeFromProfile(String profile) {

        if(profile == null || profile.isEmpty()) {
            return "none";
        }

        if(profile.equals(LaunchMode.DEVELOPMENT.getDefaultProfile())) {
            return "development";
        }

        if(profile.equals(LaunchMode.TEST.getDefaultProfile())) {
            return "test";
        }

        if(profile.equals(LaunchMode.NORMAL.getDefaultProfile())) {
            return "production";
        }

        //when no profile is matched and not empty, just return the profile name.
        return profile;
    }

    public static boolean isRebuildCheck() {
        return Boolean.getBoolean(KC_CONFIG_REBUILD_CHECK);
    }

    public static void setRebuildCheck(boolean check) {
        System.setProperty(KC_CONFIG_REBUILD_CHECK, Boolean.toString(check));
    }

    public static boolean isRebuilt() {
        return Boolean.getBoolean(KC_CONFIG_BUILT);
    }

    public static void setHomeDir(Path path) {
        System.setProperty(KC_HOME_DIR, path.toFile().getAbsolutePath());
    }

    /**
     * Do not call this method at runtime.</p>
     *
     * The method is marked as {@code synchronized} because build steps are executed in parallel.
     *
     * @return the current feature profile instance
     */
    public synchronized static Profile getCurrentOrCreateFeatureProfile() {
        Profile profile = Profile.getInstance();

        if (profile == null) {
            profile = Profile.configure(new QuarkusSingleProfileConfigResolver(), new QuarkusProfileConfigResolver());
        }

        return profile;
    }

    public static void setRebuild() {
        System.setProperty("quarkus.launch.rebuild", "true");
    }
}
