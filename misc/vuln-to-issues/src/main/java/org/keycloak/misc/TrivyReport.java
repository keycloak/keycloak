package org.keycloak.misc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TrivyReport(
        @JsonProperty("ReportID") String reportID,
        @JsonProperty("Results") List<Result> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
            @JsonProperty("Target") String target,
            @JsonProperty("Packages") List<Package> packages,
            @JsonProperty("Vulnerabilities") List<Vulnerability> vulnerabilities) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Package(
            @JsonProperty("ID") String id,
            @JsonProperty("Name") String name,
            @JsonProperty("Version") String version) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Vulnerability(
            @JsonProperty("VulnerabilityID") String vulnerabilityId,
            @JsonProperty("PkgId") String pkgId,
            @JsonProperty("PkgName") String pkgName,
            @JsonProperty("InstalledVersion") String installedVersion,
            @JsonProperty("FixedVersion") String fixedVersion,
            @JsonProperty("Status") String status,
            @JsonProperty("Severity") String severity) {
    }

}
