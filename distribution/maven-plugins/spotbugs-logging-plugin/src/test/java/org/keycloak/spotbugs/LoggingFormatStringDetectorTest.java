/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.spotbugs;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.BugCollectionBugReporter;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.config.UserPreferences;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration test that runs SpotBugs programmatically against the sample fixture
 * classes and asserts that LoggingFormatStringDetector produces exactly the expected
 * findings on broken code and zero findings on correct code.
 */
public class LoggingFormatStringDetectorTest {

    private static Collection<BugInstance> bugsFromBroken;
    private static Collection<BugInstance> bugsFromCorrect;

    @BeforeClass
    public static void runSpotBugs() throws Exception {
        loadPlugin();
        String testClassesDir = resolveTestClassesDir();
        bugsFromBroken = analyse(testClassesDir, "org.keycloak.spotbugs.samples.BrokenLoggingCalls");
        bugsFromCorrect = analyse(testClassesDir, "org.keycloak.spotbugs.samples.CorrectLoggingCalls");
    }

    // -----------------------------------------------------------------------
    // Tests — broken class must produce specific findings
    // -----------------------------------------------------------------------

    @Test
    public void tooFewArgs_isReported() {
        assertBugInMethod(bugsFromBroken, "LOGGING_FORMAT_ARG_COUNT_MISMATCH", "tooFewArgs");
    }

    @Test
    public void tooManyArgs_isReported() {
        assertBugInMethod(bugsFromBroken, "LOGGING_FORMAT_ARG_COUNT_MISMATCH", "tooManyArgs");
    }

    @Test
    public void noPlaceholderWithArg_isReported() {
        assertBugInMethod(bugsFromBroken, "LOGGING_FORMAT_ARG_COUNT_MISMATCH", "noPlaceholderWithArg");
    }

    @Test
    public void wrongSyntaxInDebugv_isReported() {
        assertBugInMethod(bugsFromBroken, "LOGGING_FORMAT_WRONG_SYNTAX", "wrongSyntaxInDebugv");
    }

    @Test
    public void wrongSyntaxInDebugf_isReported() {
        assertBugInMethod(bugsFromBroken, "LOGGING_FORMAT_WRONG_SYNTAX", "wrongSyntaxInDebugf");
    }

    @Test
    public void errorfTooFew_isReported() {
        assertBugInMethod(bugsFromBroken, "LOGGING_FORMAT_ARG_COUNT_MISMATCH", "errorfTooFew");
    }

    @Test
    public void typoInMsgFmtBracket_isReported() {
        assertBugInMethod(bugsFromBroken, "LOGGING_FORMAT_ARG_COUNT_MISMATCH", "typoInMsgFmtBracket");
    }

    @Test
    public void logfTooFew_isReported() {
        assertBugInMethod(bugsFromBroken, "LOGGING_FORMAT_ARG_COUNT_MISMATCH", "logfTooFew");
    }

    @Test
    public void wrongSyntaxInLogv_isReported() {
        assertBugInMethod(bugsFromBroken, "LOGGING_FORMAT_WRONG_SYNTAX", "wrongSyntaxInLogv");
    }

    // -----------------------------------------------------------------------
    // Tests — correct class must produce no findings
    // -----------------------------------------------------------------------

    @Test
    public void correctClass_producesNoBugs() {
        String found = bugsFromCorrect.stream()
                .map(b -> b.getBugPattern().getType() + " in " +
                        (b.getPrimaryMethod() != null ? b.getPrimaryMethod().getMethodName() : "?"))
                .collect(Collectors.joining(", "));
        Assert.assertTrue(
                "Expected no bugs in CorrectLoggingCalls but found: " + found,
                bugsFromCorrect.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Locates the SpotBugs plugin JAR produced by this module in the {@code target/}
     * directory, and registers it with SpotBugs so the detector is active.
     */
    private static void loadPlugin() throws Exception {
        File jar = locatePluginJar();
        if (jar != null && jar.isFile()) {
            Plugin plugin = Plugin.loadCustomPlugin(jar, new Project());
            DetectorFactoryCollection.resetInstance(new DetectorFactoryCollection(plugin));
        }
    }

    /**
     * Finds the first JAR in {@code target/} whose name starts with
     * {@code keycloak-spotbugs-logging-plugin} and is not a sources/javadoc/tests jar.
     */
    private static File locatePluginJar() {
        // Resolve target/ relative to the test-classes directory.
        String testClasses = resolveTestClassesDir();
        File target = new File(testClasses).getParentFile(); // target/test-classes → target/
        if (!target.isDirectory()) {
            return null;
        }
        File[] candidates = target.listFiles(f ->
                f.getName().startsWith("keycloak-spotbugs-logging-plugin")
                        && f.getName().endsWith(".jar")
                        && !f.getName().contains("sources")
                        && !f.getName().contains("javadoc")
                        && !f.getName().contains("tests"));
        return (candidates != null && candidates.length > 0) ? candidates[0] : null;
    }

    /**
     * Resolves the directory that contains the compiled test fixture classes by
     * asking the classloader for the location of a known fixture class file.
     */
    private static String resolveTestClassesDir() {
        String resourcePath = "org/keycloak/spotbugs/samples/BrokenLoggingCalls.class";
        URL url = LoggingFormatStringDetectorTest.class.getClassLoader().getResource(resourcePath);
        if (url == null) {
            throw new IllegalStateException("Cannot locate test fixture class on classpath: " + resourcePath);
        }
        // url is  file:/…/target/test-classes/org/keycloak/…/BrokenLoggingCalls.class
        // Strip the resource path suffix to get the test-classes root.
        String full = url.getPath();
        return full.substring(0, full.length() - resourcePath.length());
    }

    /**
     * Runs the SpotBugs engine over a single class and returns all reported bugs.
     *
     * @param testClassesDir absolute path to the test-classes root directory
     * @param className      fully-qualified class name to analyse
     */
    private static Collection<BugInstance> analyse(String testClassesDir, String className) throws Exception {
        // Convert class name to a relative path so SpotBugs can find the .class file.
        String classFile = testClassesDir
                + className.replace('.', File.separatorChar)
                + ".class";

        Project project = new Project();
        project.addFile(classFile);

        BugCollectionBugReporter reporter = new BugCollectionBugReporter(project);
        reporter.setPriorityThreshold(Integer.MAX_VALUE);

        try (FindBugs2 engine = new FindBugs2()) {
            engine.setProject(project);
            engine.setBugReporter(reporter);
            engine.setDetectorFactoryCollection(DetectorFactoryCollection.instance());
            engine.setAnalysisFeatureSettings(FindBugs.DEFAULT_EFFORT);
            engine.setUserPreferences(UserPreferences.createDefaultUserPreferences());
            engine.execute();
        }

        return reporter.getBugCollection().getCollection();
    }

    /**
     * Asserts that {@code bugs} contains at least one bug with the given code reported
     * in a method with the given name.
     */
    private void assertBugInMethod(Collection<BugInstance> bugs, String bugCode, String methodName) {
        boolean found = bugs.stream().anyMatch(b -> {
            if (!bugCode.equals(b.getBugPattern().getType())) {
                return false;
            }
            return b.getPrimaryMethod() != null
                    && methodName.equals(b.getPrimaryMethod().getMethodName());
        });

        if (!found) {
            String allBugs = bugs.isEmpty() ? "(none)" : bugs.stream()
                    .map(b -> b.getBugPattern().getType() + " in " +
                            (b.getPrimaryMethod() != null ? b.getPrimaryMethod().getMethodName() : "?"))
                    .collect(Collectors.joining(", "));
            Assert.fail(String.format(
                    "Expected bug %s in method '%s' but found none.%nAll bugs found: %s",
                    bugCode, methodName, allBugs));
        }
    }
}
