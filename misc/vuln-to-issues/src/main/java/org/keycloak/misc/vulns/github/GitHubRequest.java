package org.keycloak.misc.vulns.github;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.misc.vulns.JsonUtil;

import com.fasterxml.jackson.databind.JsonNode;

class GitHubRequest {

    private static final Pattern LINK_NEXT = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");

    private final HttpClient httpClient;
    private final String token;

    GitHubRequest() {
        this.httpClient = HttpClient.newHttpClient();
        this.token = GhTokenHelper.getToken();
    }

    JsonNode get(URI uri) throws GitHubApiException {
        return parseBody(sendGet(uri));
    }

    List<JsonNode> list(URI uri) throws GitHubApiException {
        List<JsonNode> allItems = new LinkedList<>();
        URI nextUri = uri;
        while (nextUri != null) {
            HttpResponse<byte[]> response = sendGet(nextUri);
            JsonNode json = parseBody(response);
            json.get("items").forEach(allItems::add);
            nextUri = parseNextLink(response).orElse(null);
        }
        return allItems;
    }

    JsonNode post(URI uri, Object body) throws GitHubApiException {
        return sendUpdate(uri, "POST", body, 201);
    }

    JsonNode patch(URI uri, Object body) throws GitHubApiException {
        return sendUpdate(uri, "PATCH", body, 200);
    }

    HttpResponse<byte[]> sendGet(URI uri) throws GitHubApiException {
        try {
            HttpRequest request = request(uri).GET().build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new IOException("GitHub API returned " + response.statusCode() + ": " + new String(response.body()));
            }
            return response;
        } catch (Exception e) {
            throw new GitHubApiException("Failed to send request to: " + uri, e);
        }
    }

    JsonNode sendUpdate(URI uri, String method, Object body, int expectedResponseCode) throws GitHubApiException {
        try {
            byte[] bodyBytes = JsonUtil.write(body);
            HttpRequest request = request(uri)
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != expectedResponseCode) {
                throw new IOException("GitHub API returned " + response.statusCode() + ": " + new String(response.body()));
            }
            return JsonUtil.read(response.body());
        } catch (Exception e) {
            throw new GitHubApiException("Failed to send request to: " + uri, e);
        }
    }


    private HttpRequest.Builder request(URI uri) {
        return HttpRequest.newBuilder().uri(uri)
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + token)
                .header("X-GitHub-Api-Version",  "2026-03-10");
    }

    private JsonNode parseBody(HttpResponse<byte[]> response) throws GitHubApiException {
        try {
            return JsonUtil.read(response.body());
        } catch (Exception e) {
            throw new GitHubApiException("Failed to parse response body", e);
        }
    }

    private Optional<URI> parseNextLink(HttpResponse<byte[]> response) {
        return response.headers().firstValue("Link").flatMap(link -> {
            Matcher m = LINK_NEXT.matcher(link);
            if (m.find()) {
                return Optional.of(URI.create(m.group(1)));
            }
            return Optional.empty();
        });
    }

}
