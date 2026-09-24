package org.keycloak.misc.vulns.cveorg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CveInfo(CveMetadata cveMetadata, Containers containers) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CveMetadata(String cveId, String state, String datePublished) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Containers(Cna cna) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cna(String title, List<Affected> affected, List<Description> descriptions) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Affected(String vendor, String product, String packageName, String packageURL) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Description(String lang, String value) {}

}
