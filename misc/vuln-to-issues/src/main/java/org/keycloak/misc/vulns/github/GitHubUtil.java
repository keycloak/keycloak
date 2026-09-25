package org.keycloak.misc.vulns.github;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.kohsuke.github.GHIssue;

public class GitHubUtil {

    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static void setIssueType(String token, GHIssue issue, String issueType) throws IOException, InterruptedException, URISyntaxException {
        URL url = issue.getUrl();
        String body = String.format("{\"type\": \"%s\"}", issueType);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(url.toURI())
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to set issue type: " + response.statusCode());
        }
    }

}
