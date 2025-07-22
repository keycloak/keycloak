package org.keycloak.test.logparser;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class TextReportGenerator implements ReportGenerator {

    public void printReport(List<GitHubRun> runs, List<TestFailure> failedTests) {
        printDivider();
        printRunSummary(runs);
        printDivider();
        printFailedTests(failedTests);
        printDivider();
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

        System.out.println("Failed steps:");
        System.out.println();
        for (RunFailure run : failedRuns) {
            System.out.println(run.details().size() + "\t" + run.run());
            for (RunFailure.FailedRunDetails details : run.details()) {
                System.out.println("\t\t - " + details.url());
            }
        }
    }

    public void printFailedTests(List<TestFailure> failedTests) {
        System.out.println("Failed tests:");
        System.out.println();
        for (TestFailure failedTest : failedTests) {
            System.out.println(failedTest.details().size() + "\t" + failedTest.test());
            for (TestFailure.FailedTestDetails details : failedTest.details()) {
                System.out.println("\t\t - " + details.url());
            }
        }
    }

    public void printDivider() {
        System.out.println("--------------------------------------------------------------------------------");
    }

}
