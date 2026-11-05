package org.keycloak.misc.vulns.github;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

public class GitHubIssueBuilder {

    private final String repositoryFullName;
    private final GitHubRequest request;
    private final GitHubIssue existing;

    private final Map<String, Object> requestBody = new LinkedHashMap<>();
    private Set<String> addLabels = new HashSet<>();

    GitHubIssueBuilder(String repositoryFullName, GitHubRequest request, GitHubIssue issue) {
        this.existing = issue;
        this.repositoryFullName = repositoryFullName;
        this.request = request;
    }

    GitHubIssueBuilder(String repositoryFullName, GitHubRequest request, String title) {
        this.existing = null;
        this.repositoryFullName = repositoryFullName;
        this.request = request;
        title(title);
    }

    public GitHubIssueBuilder title(String title) {
        return put("title", title);
    }

    public GitHubIssueBuilder body(String body) {
        return put("body", body);
    }

    public GitHubIssueBuilder type(String type) {
        return put("type", type);
    }

    public GitHubIssueBuilder reopen() {
        return put("state", "open");
    }

    public GitHubIssueBuilder addLabels(Collection<String> labels) {
        this.addLabels.addAll(labels);
        return this;
    }

    public GitHubIssue send() throws GitHubApiException {
        if (existing == null) {
            requestBody.put("labels", this.addLabels);

            URI uri = GitHubUriBuilder.create("repos", repositoryFullName, "issues").build();
            JsonNode json = request.post(uri, requestBody);
            return GitHubApi.asIssue(json);
        } else {
            String issueNumber = Integer.toString(existing.number());
            GitHubIssue updatedIssue = existing;
            if (!requestBody.isEmpty()) {
                URI uri = GitHubUriBuilder.create("repos", repositoryFullName, "issues", issueNumber).build();
                JsonNode json = request.patch(uri, requestBody);
                updatedIssue = GitHubApi.asIssue(json);
            }
            if (addLabels != null) {
                URI uri = GitHubUriBuilder.create("repos", repositoryFullName, "issues", issueNumber, "labels").build();
                request.post(uri, Map.of("labels", addLabels), 200);
                updatedIssue.labels().addAll(addLabels);
            }
            return updatedIssue;
        }
    }

    private GitHubIssueBuilder put(String key, Object value) {
        requestBody.put(key, value);
        return this;
    }

}
