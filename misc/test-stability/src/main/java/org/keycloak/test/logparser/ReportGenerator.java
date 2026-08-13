package org.keycloak.test.logparser;

import java.util.List;

public interface ReportGenerator {

    void printReport(List<GitHubRun> runs, List<TestFailure> failedTests);

}
