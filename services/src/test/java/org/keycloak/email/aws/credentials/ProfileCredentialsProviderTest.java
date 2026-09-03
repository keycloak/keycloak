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
import java.nio.file.Path;

import org.keycloak.email.aws.FakeTransport;
import org.keycloak.email.aws.TestEnvironment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Exercises the provider against real files under a temporary home, which is the only way to cover
 * what actually breaks here: the two spellings of a section header, the precedence between the two
 * files, and a secret that contains the characters a naive INI parser eats.
 */
class ProfileCredentialsProviderTest {

    /**
     * A transport that fails the test if it is called at all: reading a profile must never touch the
     * network, and a provider that fell through to an HTTP endpoint would otherwise pass unnoticed.
     */
    private final FakeTransport transport = new FakeTransport();

    @TempDir
    Path home;

    @Test
    void returnsNullWhenNoProfileFileExists() throws Exception {
        AwsCredentials credentials = new ProfileCredentialsProvider(environment()).resolve(transport);

        assertThat(credentials, nullValue());
    }

    @Test
    void readsTheDefaultProfileFromTheCredentialsFile() throws Exception {
        writeHomeFile("credentials", """
                [default]
                aws_access_key_id = AKIADEFAULT
                aws_secret_access_key = default-secret
                """);

        AwsCredentials credentials = new ProfileCredentialsProvider(environment()).resolve(transport);

        assertThat(credentials, notNullValue());
        assertThat(credentials.accessKeyId(), is("AKIADEFAULT"));
        assertThat(credentials.secretAccessKey(), is("default-secret"));
        assertThat(credentials.isTemporary(), is(false));
        assertThat(credentials.expiration(), nullValue());
    }

    /**
     * The named-profile case and the section-naming rule in one file pair: {@code [profile staging]}
     * is the config file's spelling only, so the identically named section in the credentials file
     * must not be picked up — if it were, the wrong key would come back.
     */
    @Test
    void readsTheProfileNamedByAwsProfileAndAcceptsTheProfilePrefixOnlyInTheConfigFile() throws Exception {
        writeHomeFile("credentials", """
                [default]
                aws_access_key_id = AKIADEFAULT
                aws_secret_access_key = default-secret

                [profile staging]
                aws_access_key_id = AKIAMISSPELLED
                aws_secret_access_key = misspelled-secret
                """);
        writeHomeFile("config", """
                [profile staging]
                aws_access_key_id = AKIASTAGING
                aws_secret_access_key = staging-secret
                """);

        AwsCredentials credentials = new ProfileCredentialsProvider(environment().with("AWS_PROFILE", "staging"))
                .resolve(transport);

        assertThat(credentials.accessKeyId(), is("AKIASTAGING"));
        assertThat(credentials.secretAccessKey(), is("staging-secret"));
    }

    @Test
    void honoursTheFileLocationOverridesAndMergesTheTwoFiles() throws Exception {
        Path credentialsFile = writeFile(home.resolve("elsewhere/creds.ini"),
                "[default]\naws_access_key_id = AKIAELSEWHERE\n");
        Path configFile = writeFile(home.resolve("elsewhere/conf.ini"),
                "[default]\naws_secret_access_key = elsewhere-secret\n");
        writeHomeFile("credentials", """
                [default]
                aws_access_key_id = AKIAHOME
                aws_secret_access_key = home-secret
                """);

        AwsCredentials credentials = new ProfileCredentialsProvider(environment()
                .with("AWS_SHARED_CREDENTIALS_FILE", credentialsFile.toString())
                .with("AWS_CONFIG_FILE", configFile.toString()))
                .resolve(transport);

        assertThat(credentials.accessKeyId(), is("AKIAELSEWHERE"));
        assertThat(credentials.secretAccessKey(), is("elsewhere-secret"));
    }

    @Test
    void reportsAProfileWithASessionTokenAsTemporaryButNotExpiring() throws Exception {
        writeHomeFile("credentials", """
                [default]
                aws_access_key_id = ASIATEMPORARY
                aws_secret_access_key = temporary-secret
                aws_session_token = FwoGZXIvYXdzEJr//////////wEaDExAMPLE=
                """);

        AwsCredentials credentials = new ProfileCredentialsProvider(environment()).resolve(transport);

        assertThat(credentials.sessionToken(), is("FwoGZXIvYXdzEJr//////////wEaDExAMPLE="));
        assertThat(credentials.isTemporary(), is(true));
        assertThat(credentials.expiration(), nullValue());
    }

    /**
     * The {@code #} inside the secret is the point: it is a legal character in an AWS secret access
     * key, so it must survive the parser rather than be read as the start of a trailing comment.
     */
    @Test
    void parsesCommentsBlankLinesWhitespaceCrlfAndASecretContainingHashAndEquals() throws Exception {
        String secret = "wJal#rXUtnFEMI/K7MDENG=bPxRfi#CYEXAMPLEKEY";
        writeHomeFile("credentials", String.join("\r\n",
                "# a comment before any section",
                "; and the other comment character",
                "",
                "[default]",
                "    aws_access_key_id   =   AKIASUPERSEDED   ",
                "   # an indented comment inside the section",
                "aws_secret_access_key = " + secret,
                "region = eu-south-1",
                "aws_access_key_id = AKIALAST",
                "",
                "[other]",
                "aws_access_key_id = AKIAOTHER",
                ""));

        AwsCredentials credentials = new ProfileCredentialsProvider(environment()).resolve(transport);

        assertThat(credentials.accessKeyId(), is("AKIALAST"));
        assertThat(credentials.secretAccessKey(), is(secret));
    }

    @Test
    void failsWhenTheProfileHasTheKeyIdButNoSecret() throws Exception {
        writeHomeFile("credentials", """
                [default]
                aws_access_key_id = AKIAHALFCONFIGURED
                """);
        ProfileCredentialsProvider provider = new ProfileCredentialsProvider(environment());

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class, () -> provider.resolve(transport));

        assertThat(failure.getMessage(), containsString("aws_secret_access_key"));
        assertThat(failure.getMessage(), containsString("default"));
    }

    @Test
    void treatsAKeyWithAnEmptyValueAsUnset() throws Exception {
        writeHomeFile("credentials", """
                [default]
                aws_access_key_id =
                aws_secret_access_key =
                """);

        AwsCredentials credentials = new ProfileCredentialsProvider(environment()).resolve(transport);

        assertThat(credentials, nullValue());
    }

    @Test
    void returnsNullWhenTheRequestedProfileIsAbsentFromAFileThatExists() throws Exception {
        writeHomeFile("credentials", """
                [default]
                aws_access_key_id = AKIADEFAULT
                aws_secret_access_key = default-secret
                """);

        AwsCredentials credentials = new ProfileCredentialsProvider(environment().with("AWS_PROFILE", "absent"))
                .resolve(transport);

        assertThat(credentials, nullValue());
    }

    @Test
    void prefersTheCredentialsFileOverTheConfigFileKeyByKey() throws Exception {
        writeHomeFile("credentials", """
                [default]
                aws_access_key_id = AKIAFROMCREDENTIALS
                aws_secret_access_key = credentials-secret
                """);
        writeHomeFile("config", """
                [default]
                aws_access_key_id = AKIAFROMCONFIG
                aws_secret_access_key = config-secret
                aws_session_token = token-only-in-config
                """);

        AwsCredentials credentials = new ProfileCredentialsProvider(environment()).resolve(transport);

        assertThat(credentials.accessKeyId(), is("AKIAFROMCREDENTIALS"));
        assertThat(credentials.secretAccessKey(), is("credentials-secret"));
        assertThat(credentials.sessionToken(), is("token-only-in-config"));
    }

    private TestEnvironment environment() {
        return TestEnvironment.empty().withUserHome(home);
    }

    private void writeHomeFile(String name, String content) throws IOException {
        writeFile(home.resolve(".aws").resolve(name), content);
    }

    /**
     * The AWS SDKs read {@code aws.profile} before {@code AWS_PROFILE}. Reading only the variable
     * would make this provider send email as one identity while every other AWS client in the same
     * JVM used another.
     */
    @Test
    void prefersTheProfileSystemPropertyOverTheEnvironmentVariable() throws Exception {
        writeHomeFile("credentials", """
                [default]
                aws_access_key_id = AKIADEFAULT
                aws_secret_access_key = default-secret

                [chosen]
                aws_access_key_id = AKIACHOSEN
                aws_secret_access_key = chosen-secret
                """);
        TestEnvironment environment = environment()
                .with("AWS_PROFILE", "default")
                .withProperty("aws.profile", "chosen");

        assertThat(new ProfileCredentialsProvider(environment).resolve(null).accessKeyId(), is("AKIACHOSEN"));
    }

    /** Same rule for the file locations. */
    @Test
    void prefersTheCredentialsFileSystemPropertyOverTheEnvironmentVariable() throws Exception {
        Path chosen = home.resolve("chosen-credentials");
        writeFile(chosen, """
                [default]
                aws_access_key_id = AKIAFROMPROPERTY
                aws_secret_access_key = property-secret
                """);
        Path other = home.resolve("other-credentials");
        writeFile(other, """
                [default]
                aws_access_key_id = AKIAFROMVARIABLE
                aws_secret_access_key = variable-secret
                """);
        TestEnvironment environment = environment()
                .with("AWS_SHARED_CREDENTIALS_FILE", other.toString())
                .withProperty("aws.sharedCredentialsFile", chosen.toString());

        assertThat(new ProfileCredentialsProvider(environment).resolve(null).accessKeyId(), is("AKIAFROMPROPERTY"));
    }

    private static Path writeFile(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        return Files.writeString(file, content);
    }
}
