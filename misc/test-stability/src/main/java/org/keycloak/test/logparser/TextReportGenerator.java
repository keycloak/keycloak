package org.keycloak.test.logparser;

import java.util.List;

public class TextReportGenerator implements ReportGenerator {

    public void printReport(List<GitHubRun> runs, List<TestFailure> failedTests) {
        printDivider();
        printRunSummary(runs);
        printDivider();
        printFailedTests(failedTests);
        printDivider();
    }

    private void printRunSummary(List<GitHubRun> runs) {
        List<RunFailure> failedRuns = Utils.toRunFailures(runs);

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
