package org.keycloak.misc.vulns.github;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class GitHubUriBuilder {

    private static final String BASE_URL = "https://api.github.com";
    private List<String> path = new LinkedList<>();
    private Map<String, String> query = new HashMap<>();

    GitHubUriBuilder(String... path) {
        this.path.addAll(Arrays.asList(path));
    }

    static GitHubUriBuilder create(String... path) {
        return new GitHubUriBuilder(path);
    }

    GitHubUriBuilder query(String k, String v) {
        query.put(k, v);
        return this;
    }

    GitHubUriBuilder query(String k, long v) {
        query.put(k, Long.toString(v));
        return this;
    }

    URI build() throws GitHubApiException {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(BASE_URL);
            path.forEach(p -> sb.append('/').append(p));
            char delimiter = '?';
            for (Map.Entry<String, String> e : query.entrySet()) {
                sb.append(delimiter).append(e.getKey()).append('=').append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
                delimiter = '&';
            }
            return new URI(sb.toString());
        } catch (Exception e) {
            throw new GitHubApiException("Failed to build uri", e);
        }
    }

}
