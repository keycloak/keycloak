package org.keycloak.misc.vulns.snyk;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SnykReport(
        List<SnykVulnerability> vulnerabilities
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SnykVulnerability(
            String title,
            List<String> fixedIn,
            String severity,
            String packageName,
            String packageManager,
            String version,
            Map<String, List<String>> identifiers,
            List<String> from
    ) {
    }

}
