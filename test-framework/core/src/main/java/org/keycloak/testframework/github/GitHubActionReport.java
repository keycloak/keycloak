package org.keycloak.testframework.github;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.keycloak.testframework.config.Config;

import org.junit.jupiter.api.extension.ExtensionContext;

public class GitHubActionReport {

    private static final String GITHUB_STEP_SUMMARY = System.getenv("GITHUB_STEP_SUMMARY");
    private static final String GITHUB_SERVER_URL = System.getenv("GITHUB_SERVER_URL");
    private static final String GITHUB_REPOSITORY = System.getenv("GITHUB_REPOSITORY");
    private static final String GITHUB_SHA = System.getenv("GITHUB_SHA");
    private static final String GIT_ROOT = findGitRoot();

    private final boolean enabled;
    private final File gitHubStepSummary;

    private final long slowTestClassTimeout;
    private final long slowTestTimeout;

    private long testClassStartedAt;
    private long testStartedAt;

    private List<Failure> failures = new LinkedList<>();
    private List<Slow> slowTests = new LinkedList<>();

    public GitHubActionReport() {
        this.gitHubStepSummary = GITHUB_STEP_SUMMARY != null ? new File(GITHUB_STEP_SUMMARY) : null;
        this.enabled = Config.get("kc.test.github.enabled", true, Boolean.class) && gitHubStepSummary != null;
        this.slowTestClassTimeout = TimeUnit.SECONDS.toMillis(Config.get("kc.test.github.slow.class", 120L, Long.class));
        this.slowTestTimeout = TimeUnit.SECONDS.toMillis(Config.get("kc.test.github.slow.method", 30L, Long.class));
    }

    public void onClassStart() {
        if (enabled) {
            testClassStartedAt = System.currentTimeMillis();
        }
    }

    public void onClassSuccess(ExtensionContext context) {
        if (enabled) {
            if (slowTestClassTimeout >= -1) {
                long executionTime = System.currentTimeMillis() - testClassStartedAt;
                if (executionTime > slowTestClassTimeout) {
                    Class<?> testClass = context.getRequiredTestClass();
                    String file = findJavaClass(testClass);
                    String link = getLink(file, -1);
                    slowTests.add(new Slow(context.getRequiredTestClass().getName(), null, executionTime, link));
                }
            }
        }
    }

    public void onClassError(ExtensionContext context) {
        if (enabled) {
            onError(context, false);
        }
    }

    public void onMethodStart() {
        if (enabled && slowTestTimeout >= -1) {
            testStartedAt = System.currentTimeMillis();
        }
    }

    public void onMethodSuccess(ExtensionContext context) {
        if (enabled) {
            if (slowTestTimeout >= -1) {
                long executionTime = System.currentTimeMillis() - testStartedAt;
                if (executionTime > slowTestTimeout) {
                    Class<?> testClass = context.getRequiredTestClass();
                    String file = findJavaClass(testClass);
                    String link = getLink(file, -1);
                    slowTests.add(new Slow(context.getRequiredTestClass().getName(), context.getRequiredTestMethod().getName(), executionTime, link));
                }
            }
        }
    }

    public void onMethodFailed(ExtensionContext context) {
        if (enabled) {
            onError(context, true);
        }
    }

    public void printSummary() {
        if (enabled && (!failures.isEmpty() || !slowTests.isEmpty())) {
            try {
                PrintWriter printWriter = new PrintWriter(new FileWriter(gitHubStepSummary, true));

                if (!failures.isEmpty()) {
                    printWriter.println("## :x: Failed tests");
                    printWriter.println("| Test class | Test method | Line | Failure |");
                    printWriter.println("| ---------- | ----------- | ---- | ------- |");

                    failures.stream().sorted(Comparator.comparing(Failure::className)).forEach(f ->
                            printWriter.println("| " + createLink(f.className(), f.link()) + " | " + (f.methodName() != null ? f.methodName() : "") + " | " + (f.line() >= 0 ? f.line() : "") + " | `" + f.message() + "` |")
                    );
                }

                if (!slowTests.isEmpty()) {
                    printWriter.println("## :hourglass: Slow tests detected");
                    printWriter.println("| Test class | Test method | Execution time (s) |");
                    printWriter.println("| ---------- | ----------- | -------------- |");

                    slowTests.stream().sorted(Comparator.comparing(Slow::executionTime).reversed()).forEach(s ->
                            printWriter.println("| " + createLink(s.className(), s.link()) + " | " + (s.methodName() != null ? s.methodName() : "") + " | " + (s.executionTime() / 1000) + " |")
                    );
                }

                printWriter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void onError(ExtensionContext context, boolean method) {
        Optional<Throwable> executionException = context.getExecutionException();
        if (executionException.isPresent()) {
            Class<?> testClass = context.getRequiredTestClass();
            String file = findJavaClass(testClass);

            Method testMethod = method ? context.getRequiredTestMethod() : null;
            Throwable throwable = executionException.get();
            String message = throwable.getMessage();
            int line = findLine(testClass, testMethod, throwable);

            String link = getLink(file, line);

            failures.add(new Failure(testClass.getName(), testMethod != null ? testMethod.getName() : null, message, link, line));
        }
    }

    private String findJavaClass(Class<?> testClass) {
        if (GIT_ROOT == null) {
            return null;
        }

        String classFile = testClass.getResource("/" + testClass.getName().replace('.', '/') + ".class").getFile();
        return classFile.replace(GIT_ROOT + "/", "").replace("target/test-classes", "src/test/java").replace(".class", ".java");
    }

    private String getLink(String file, int line) {
        if (file == null) {
            return null;
        }
        String link = GITHUB_SERVER_URL + "/" + GITHUB_REPOSITORY + "/blob/" + GITHUB_SHA + "/" + file;
        if (line >= 0) {
            link += "#L" + line;
        }
        return link;
    }

    private static String findGitRoot() {
        File file = new File(System.getProperty("user.dir"));
        while (file != null && file.isDirectory()) {
            if (new File(file, ".git").isDirectory()) {
                return file.getAbsolutePath();
            }
            file = file.getParentFile();
        }
        return null;
    }

    private int findLine(Class<?> testClass, Method testMethod, Throwable throwable) {
        for (StackTraceElement stackTraceElement : throwable.getStackTrace()) {
            if (stackTraceElement.getClassName().equals(testClass.getName()) && (testMethod == null || stackTraceElement.getMethodName().equals(testMethod.getName()))) {
                return stackTraceElement.getLineNumber();
            }
        }
        return -1;
    }

    private String createLink(String text, String link) {
        if (link == null) {
            return text;
        }
        return "[" + text + "]" + "(" + link + ")";
    }

    private record Slow(String className, String methodName, long executionTime, String link) {}

    private record Failure(String className, String methodName, String message, String link, int line) {}

}
