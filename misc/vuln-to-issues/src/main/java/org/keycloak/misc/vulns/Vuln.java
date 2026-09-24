package org.keycloak.misc.vulns;

import org.kohsuke.github.GHIssue;

import java.util.Set;

public record Vuln(
        String cveId,
        String severity,
        String title,
        String packageName,
        String packageManager,
        Set<String> installedVersions,
        Set<String> fixedVersions,
        Set<String> targets,
        Set<String> affectedStreams,
        Set<GHIssue> issues,
        Set<Tool> tools
) {
}
