package org.keycloak.misc.vulns.github;

import java.util.Set;

public record GitHubIssue(int id, int number, String title, String state, String stateReason, String url, String htmlUrl, Set<String> labels) {
}
