package org.keycloak.misc.vulns.github;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

public class GitHubApi {

    private static final int DEFAULT_PER_PAGE = 100;

    private final GitHubRequest request;
    private final String repository;

    public GitHubApi(String repository) {
        this.repository = repository;
        this.request = new GitHubRequest();
    }

    public String getRepository() {
        return repository;
    }

    public long getRepositoryId() throws GitHubApiException {
        URI uri = GitHubUriBuilder.create("repos", repository).build();
        JsonNode json = request.get(uri);
        return json.get("id").asLong();
    }

    public GitHubIssue getIssue(int issueNumber) throws GitHubApiException {
        URI uri = GitHubUriBuilder.create("repos", repository, "issues", Integer.toString(issueNumber)).build();
        JsonNode json = request.get(uri);
        return asIssue(json);
    }

    public Set<String> searchLabels(String query) throws GitHubApiException {
        long repositoryId = getRepositoryId();
        URI uri = GitHubUriBuilder
                .create("search", "labels")
                .query("repository_id", repositoryId)
                .query("q", query)
                .query("per_page", DEFAULT_PER_PAGE)
                .build();
        List<JsonNode> items = request.list(uri);
        return items.stream().map(GitHubApi::asName).collect(Collectors.toSet());
    }

    public List<GitHubIssue> searchIssues(String query) throws GitHubApiException {
        URI uri = GitHubUriBuilder
                .create("search", "issues")
                .query("q", query)
                .query("advanced_search", "true")
                .query("per_page", DEFAULT_PER_PAGE)
                .build();
        List<JsonNode> items = request.list(uri);
        return items.stream().map(GitHubApi::asIssue).toList();
    }

    public GitHubIssueBuilder createIssue(String title) {
        return new GitHubIssueBuilder(repository, request, title);
    }

    public GitHubIssueBuilder updateIssue(GitHubIssue issue) {
        return new GitHubIssueBuilder(repository, request, issue);
    }

    static String asName(JsonNode json) {
        return json.get("name").asText();
    }

    static GitHubIssue asIssue(JsonNode json) {
        int id = json.get("id").asInt();
        int number = json.get("number").asInt();
        String title = json.get("title").asText();
        String state = json.get("state").asText();
        String stateReason = json.get("state_reason").asText();
        String url = json.get("url").asText();
        String htmlUrl = json.get("html_url").asText();
        Set<String> labels = json.get("labels").valueStream()
                .map(GitHubApi::asName)
                .collect(Collectors.toSet());
        return new GitHubIssue(id, number, title, state, stateReason, url, htmlUrl, labels);
    }

}
