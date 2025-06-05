package org.keycloak.test.logparser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRun(String name, String conclusion, String databaseId, List<GitHubRunJob> jobs) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubRunJob(String name, String conclusion, String databaseId, String url, List<GitHubRunJobStep> steps) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubRunJobStep(String name, String conclusion, String databaseId) {}

}
