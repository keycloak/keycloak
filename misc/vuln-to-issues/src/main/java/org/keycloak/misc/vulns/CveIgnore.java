package org.keycloak.misc.vulns;

import java.util.List;

public record CveIgnore(Severity ignoreLevel, List<Ignored> vulnerabilities) {

    public record Ignored(String cveId, String packageName, String statement, String gitHubIssue) {}

}
