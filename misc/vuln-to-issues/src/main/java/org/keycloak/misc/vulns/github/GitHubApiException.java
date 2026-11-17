package org.keycloak.misc.vulns.github;

public class GitHubApiException extends Exception {

    public GitHubApiException(String message, Exception exception) {
        super(message, exception);
    }

}
