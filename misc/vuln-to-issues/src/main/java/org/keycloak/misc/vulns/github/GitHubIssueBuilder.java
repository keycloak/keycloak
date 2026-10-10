package org.keycloak.misc.vulns.github;

import java.net.URI;
import java.util.Arrays;
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
    private Set<String> labels;

    GitHubIssueBuilder(String repositoryFullName, GitHubRequest request, GitHubIssue issue) {
        this.existing = issue;
        this.repositoryFullName = repositoryFullName;
        this.request = request;
        this.labels = new HashSet<>(existing.labels());
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

    public GitHubIssueBuilder completed() {
        return put("state", "closed").put("state_reason", "completed");
    }

    public GitHubIssueBuilder notPlanned() {
        return put("state", "closed").put("state_reason", "not_planned");
    }

    public GitHubIssueBuilder label(String... labels) {
        return label(Arrays.asList(labels));
    }

    public GitHubIssueBuilder label(Collection<String> labels) {
        if (this.labels == null) {
            this.labels = new HashSet<>();
        }
        this.labels.addAll(labels);
        return this;
    }

    public GitHubIssueBuilder removeLabel(String... labels) {
        if (this.labels != null) {
            Arrays.asList(labels).forEach(this.labels::remove);
        }
        return this;
    }

    public GitHubIssueBuilder clearLabels() {
        this.labels.clear();
        return this;
    }

    public GitHubIssue send() throws GitHubApiException {
        if ((existing == null && this.labels != null) || (existing != null && !existing.labels().equals(this.labels))) {
            requestBody.put("labels", this.labels);
        }

        if (existing == null) {
            URI uri = GitHubUriBuilder.create("repos", repositoryFullName, "issues").build();
            JsonNode json = request.post(uri, requestBody);
            return GitHubApi.asIssue(json);
        } else {
            URI uri = GitHubUriBuilder.create("repos", repositoryFullName, "issues", Integer.toString(existing.number())).build();
            JsonNode json = request.patch(uri, requestBody);
            return GitHubApi.asIssue(json);
        }
    }

    private GitHubIssueBuilder put(String key, Object value) {
        requestBody.put(key, value);
        return this;
    }

}
