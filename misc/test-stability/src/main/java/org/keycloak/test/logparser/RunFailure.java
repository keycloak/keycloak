package org.keycloak.test.logparser;

import java.util.List;

public record RunFailure(String run, List<FailedRunDetails> details) {

    public record FailedRunDetails(String runName, String runId, String jobName, String jobId, String url) {}

}
