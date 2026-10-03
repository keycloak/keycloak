package org.keycloak.misc.vulns;

import java.util.List;

public record CveIgnore(List<Ignored> vulnerabilities) {

    public record Ignored(String cveId, String statement, String gitHubIssue) {}

}
