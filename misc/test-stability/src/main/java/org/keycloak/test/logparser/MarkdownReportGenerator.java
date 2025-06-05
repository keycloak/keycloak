package org.keycloak.test.logparser;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MarkdownReportGenerator implements ReportGenerator {

    public void printReport(List<GitHubRun> runs, List<TestFailure> failedTests) {
        printRunSummary(runs);
        printFailedTests(failedTests);
    }

    private void printRunSummary(List<GitHubRun> runs) {
        List<RunFailure> failedRuns = new LinkedList<>();
        for (GitHubRun run : runs) {
            for (GitHubRun.GitHubRunJob job : run.jobs()) {
                if (job.conclusion().equals("failure")) {
                    for (GitHubRun.GitHubRunJobStep step : job.steps()) {
                        if (step.conclusion().equals("failure")) {
                            String fullName = run.name() + " / " + job.name() + " / " + step.name();
                            RunFailure failedRun = failedRuns.stream().filter(f -> f.run().equals(fullName)).findFirst().or(() -> {
                                RunFailure fr = new RunFailure(fullName, new LinkedList<>());
                                failedRuns.add(fr);
                                return Optional.of(fr);
                            }).get();

                            failedRun.details().add(new RunFailure.FailedRunDetails(run.name(), run.databaseId(), job.name(), job.databaseId(), job.url()));
                        }
                    }
                }
            }
        }

        System.out.println("# Failed steps");
        System.out.println("| Num | Step | Failures | ");
        System.out.println("| --- | ---- | -------- | ");

        for (RunFailure run : failedRuns) {
            String failures = run.details().stream()
                    .map(r -> "[" + r.runId() + "](" + r.url() + ")")
                    .collect(Collectors.joining(" "));
            System.out.println("| " + run.details().size() + " | " + run.run() + " | " + failures + " |");
        }
    }

    public void printFailedTests(List<TestFailure> failedTests) {
        System.out.println("# Failed tests");
        System.out.println("| Num | Test | Failures | ");
        System.out.println("| --- | ---- | -------- | ");
        for (TestFailure failedTest : failedTests) {
            String failures = failedTest.details().stream()
                    .map(r -> "[" + r.runId() + "](" + r.url() + ")")
                    .collect(Collectors.joining(" "));
            System.out.println("| " + failedTest.details().size() + " | " + failedTest.test() + " | " + failures + " |");
        }
    }

}
