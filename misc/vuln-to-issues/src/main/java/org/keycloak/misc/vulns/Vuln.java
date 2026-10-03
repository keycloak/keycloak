package org.keycloak.misc.vulns;

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
        Set<Tool> tools
) {
}
