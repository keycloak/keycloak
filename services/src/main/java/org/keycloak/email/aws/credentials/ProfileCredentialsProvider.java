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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.keycloak.email.aws.AwsHttpTransport;

import org.jboss.logging.Logger;

/**
 * Credentials from the shared AWS profile files — {@code ~/.aws/credentials} and {@code ~/.aws/config},
 * the pair every AWS tool on a developer machine or a hand-configured VM reads.
 * <p>
 * Only a static key pair is understood here: {@code aws_access_key_id}, {@code aws_secret_access_key}
 * and an optional {@code aws_session_token}, which do not expire on their own. A profile that instead
 * points at a role or at SSO carries none of those keys, and is reported as "not configured" so the
 * chain moves on to a source that can actually produce credentials.
 * <p>
 * Two things about the file format are easy to get wrong and are the reason this class parses the
 * files itself rather than reaching for a generic INI reader:
 * <ul>
 * <li>The two files spell a section differently. The credentials file names a profile
 * {@code [myprofile]}, the config file names the same profile {@code [profile myprofile]} — except
 * {@code default}, which is {@code [default]} in both.</li>
 * <li>{@code #} and {@code ;} start a comment only at the start of a line. Both are legal characters
 * in a secret access key, and a parser that strips "trailing comments" truncates the secret into a
 * SES {@code SignatureDoesNotMatch} that says nothing about the file it came from.</li>
 * </ul>
 * Where both files set the same key, the credentials file wins, key by key, as in the AWS SDKs.
 */
public final class ProfileCredentialsProvider implements AwsCredentialsProvider {

    private static final Logger logger = Logger.getLogger(ProfileCredentialsProvider.class);

    private static final String DEFAULT_PROFILE = "default";
    private static final String ACCESS_KEY_ID = "aws_access_key_id";
    private static final String SECRET_ACCESS_KEY = "aws_secret_access_key";
    private static final String SESSION_TOKEN = "aws_session_token";

    private final AwsEnvironment environment;

    public ProfileCredentialsProvider(AwsEnvironment environment) {
        this.environment = environment;
    }

    /**
     * @param transport unused: a profile is read from disk, this source never leaves the machine
     * @return the profile's key pair, or {@code null} when no file names the profile or the profile
     *         holds no static key material at all
     * @throws AwsCredentialsException when a file exists but cannot be read, or when the profile sets
     *         one half of the key pair and not the other — a typo that must not be mistaken for an
     *         unconfigured machine
     */
    @Override
    public AwsCredentials resolve(AwsHttpTransport transport) throws AwsCredentialsException {
        String profile = environment.setting("aws.profile", "AWS_PROFILE");
        if (profile == null) {
            profile = DEFAULT_PROFILE;
        }
        Path credentialsFile = fileLocation("aws.sharedCredentialsFile", "AWS_SHARED_CREDENTIALS_FILE", "credentials");
        Path configFile = fileLocation("aws.configFile", "AWS_CONFIG_FILE", "config");

        Map<String, String> settings = new HashMap<>(readProfile(credentialsFile, profile));
        readProfile(configFile, configSection(profile)).forEach(settings::putIfAbsent);

        String accessKeyId = setting(settings, ACCESS_KEY_ID);
        String secretAccessKey = setting(settings, SECRET_ACCESS_KEY);
        if (accessKeyId == null && secretAccessKey == null) {
            logger.debugf("No AWS profile credentials for profile '%s' in %s or %s", profile, credentialsFile, configFile);
            return null;
        }
        if (accessKeyId == null || secretAccessKey == null) {
            throw new AwsCredentialsException("The AWS profile '" + profile + "' is incomplete: "
                    + (accessKeyId == null ? ACCESS_KEY_ID : SECRET_ACCESS_KEY) + " is not set in "
                    + credentialsFile + " or " + configFile);
        }
        return new AwsCredentials(accessKeyId, secretAccessKey, setting(settings, SESSION_TOKEN), null);
    }

    @Override
    public String name() {
        return "profile file";
    }

    private Path fileLocation(String systemProperty, String variable, String defaultFileName) {
        String override = environment.setting(systemProperty, variable);
        return override != null ? Paths.get(override) : environment.userHome().resolve(".aws").resolve(defaultFileName);
    }

    /** The config file spells every profile but {@code default} as {@code [profile <name>]}. */
    private static String configSection(String profile) {
        return DEFAULT_PROFILE.equals(profile) ? DEFAULT_PROFILE : "profile " + profile;
    }

    private static Map<String, String> readProfile(Path file, String section) throws AwsCredentialsException {
        String content;
        try {
            content = Files.readString(file);
        } catch (NoSuchFileException e) {
            return Map.of();
        } catch (IOException e) {
            // Read first, then classify the failure. An existence check up front would file a
            // present-but-unreadable profile under "not configured", and the chain would move on to
            // another source instead of naming the file the operator has to fix.
            throw new AwsCredentialsException("The AWS profile file " + file + " exists but cannot be read", e);
        }
        return parseSection(content, section);
    }

    /**
     * Returns the keys of {@code section}, empty when the section is not in {@code content}.
     * <p>
     * Strict on purpose, and narrow on purpose: keys are lowercased, values keep every character
     * after the first {@code =} (secrets contain {@code =}, {@code #} and {@code ;}), a later
     * duplicate within a section overwrites an earlier one, and everything else — unknown keys,
     * other sections, lines without a {@code =} — is ignored rather than rejected, because this
     * file is shared with tools that write keys this provider knows nothing about.
     */
    private static Map<String, String> parseSection(String content, String section) {
        Map<String, String> settings = new HashMap<>();
        boolean inSection = false;
        for (String rawLine : content.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.charAt(0) == '#' || line.charAt(0) == ';') {
                continue;
            }
            if (line.charAt(0) == '[') {
                int end = line.indexOf(']');
                inSection = end > 0 && section.equals(line.substring(1, end).trim());
                continue;
            }
            int separator = inSection ? line.indexOf('=') : -1;
            if (separator > 0) {
                settings.put(line.substring(0, separator).trim().toLowerCase(Locale.ROOT),
                        line.substring(separator + 1).trim());
            }
        }
        return settings;
    }

    /**
     * A key present with an empty value counts as unset, for the reason
     * {@link AwsEnvironment#value(String)} gives: empty credentials fail far from here, with a
     * signature error that points at nothing.
     */
    private static String setting(Map<String, String> settings, String key) {
        String value = settings.get(key);
        return value == null || value.isBlank() ? null : value;
    }
}
