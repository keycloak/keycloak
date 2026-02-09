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

package org.keycloak.it.cli.dist;

import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.DryRun;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.WithEnvVars;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;

@DryRun
@DistributionTest
@RawDistOnly(reason = "No need to test script again on container")
@WithEnvVars({"PRINT_ENV", "true"})
@Tag(DistributionTest.WIN)
public class JavaOptsScriptTest {

    private static final String DEFAULT_OPTS = "(?:-\\S+ )*-XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m -Dfile.encoding=UTF-8(?: -\\S+)*";

    @Test
    @Launch({"start", "--optimized"})
    void testDefaultJavaOpts(LaunchResult result) {
        String output = result.getOutput();
        assertThat(output, matchesPattern("(?s).*Using JAVA_OPTS: " + DEFAULT_OPTS + ".*"));
        assertThat(output, containsString("-Xms64m -Xmx512m"));
    }

    @Test
    @Launch({"start", "--optimized"})
    @WithEnvVars({"KC_RUN_IN_CONTAINER", "true"})
    void testDefaultJavaHeapContainerOpts(LaunchResult result) {
        String output = result.getOutput();
        assertThat(output, matchesPattern("(?s).*Using JAVA_OPTS: " + DEFAULT_OPTS + ".*"));
        assertThat(output, not(containsString("-Xms64m -Xmx512m")));
        assertThat(output, containsString("-XX:MaxRAMPercentage=70 -XX:MinRAMPercentage=70 -XX:InitialRAMPercentage=50"));
    }

    @Test
    @Launch({"start", "--optimized"})
    @WithEnvVars({"JAVA_OPTS_KC_HEAP", "-Xms128m"})
    void testCustomJavaHeapContainerOpts(LaunchResult result) {
        String output = result.getOutput();
        assertThat(output, matchesPattern("(?s).*Using JAVA_OPTS: " + DEFAULT_OPTS + ".*"));
        assertThat(output, not(containsString("-Xms64m -Xmx512m")));
        assertThat(output, not(containsString("-XX:MaxRAMPercentage=70 -XX:MinRAMPercentage=70 -XX:InitialRAMPercentage=50")));
        assertThat(output, containsString("JAVA_OPTS_KC_HEAP already set in environment; overriding default settings"));
        assertThat(output, containsString(" -Xms128m "));
    }

    @Test
    @Launch({"start", "--optimized"})
    @WithEnvVars({"JAVA_OPTS_KC_HEAP", "-Xms128m", "JAVA_OPTS", "-Xmx256m"})
    void testCustomJavaHeapContainerOptsWithCustomJavaOpts(LaunchResult result) {
        String output = result.getOutput();
        assertThat(output, not(containsString("JAVA_OPTS_KC_HEAP already set in environment; overriding default settings with values")));
        assertThat(output, not(containsString("-Xms128m")));

        assertThat(output, containsString("JAVA_OPTS already set in environment; overriding default settings"));
        assertThat(output, containsString(" -Xmx256m"));
    }

    @Test
    @Launch({"start", "--optimized"})
    @WithEnvVars({ "JAVA_OPTS", "-Dfoo=bar"})
    void testJavaOpts(LaunchResult result) {
        String output = result.getOutput();
        assertThat(output, containsString("JAVA_OPTS already set in environment; overriding default settings"));
        assertThat(output, containsString(String.format("Using JAVA_OPTS: %s-Dfoo=bar",
                OS.WINDOWS.isCurrentOs() ? "-Dprogram.name=kc.bat " : "")));
    }

    @Test
    @Launch({"start", "--optimized"})
    @WithEnvVars({ "JAVA_OPTS_APPEND", "-Dfoo=bar"})
    void testJavaOptsAppend(LaunchResult result) {
        String output = result.getOutput();
        assertThat(output, containsString("Appending additional Java properties to JAVA_OPTS"));
        assertThat(output, matchesPattern(String.format("(?s).*Using JAVA_OPTS: %s%s -Dfoo=bar\\r?\\n.*",
                OS.WINDOWS.isCurrentOs() ? "-Dprogram.name=kc.bat " : "", DEFAULT_OPTS)));
    }

    @Test
    @Launch({"start", "--optimized"})
    @WithEnvVars({ "JAVA_ADD_OPENS", "-Dfoo=bar"})
    void testJavaAddOpens(LaunchResult result) {
        String output = result.getOutput();
        assertThat(output, containsString("JAVA_ADD_OPENS already set in environment; overriding default settings"));
        assertThat(output, not(containsString("--add-opens")));
        assertThat(output, matchesPattern(String.format("(?s).*Using JAVA_OPTS: %s%s -Dfoo=bar.*",
                OS.WINDOWS.isCurrentOs() ? "-Dprogram.name=kc.bat " : "", DEFAULT_OPTS)));
    }

    @Test
    @Launch({ "start-dev", "-Dpicocli.trace=info" })
    void testPicocliClosuresDisabled(LaunchResult result) {
        String output = result.getErrorOutput(); // not sure why picocli logs are printed to err
        assertThat(output, containsString("DefaultFactory: groovy Closures in annotations are disabled and will not be loaded"));
    }

    @EnabledOnOs(value = { OS.WINDOWS }, disabledReason = "different path behaviour on Windows.")
    @Test
    @Launch({"start-dev", "--optimized"})
    void testKcHomeDirPathFormat(LaunchResult result) {
        String output = result.getOutput();
        assertThat(output, containsString("kc.home.dir="));
        assertThat(output, matchesPattern("(?s).*kc\\.home\\.dir=\"[A-Z]:/.*/target/kc-tests/keycloak-\\d+\\.\\d+\\.\\d+.*?/bin/\\.\\.\".*"));
    }

}
