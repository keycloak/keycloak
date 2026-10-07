package org.keycloak.misc.vulns;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.misc.vulns.github.GitHubIssues;

public class Report {

    public static String createReport(List<Vuln> vulns, GitHubIssues gitHubIssues) {
        StringBuilder sb = new StringBuilder();

        sb.append("## Vulnerabilities\n\n");
        sb.append("| Severity | CVE ID | Package | Affects | Installed versions | Fixed versions | GitHub Issues | Detected by |").append('\n');
        sb.append("| -------- | ------ | ------- | ------- | ------------------ | -------------- | ------------- | ----------- |").append('\n');

        vulns.stream().sorted(Comparator.comparing(Vuln::severity).thenComparing(Vuln::cveId)).forEach(v -> {
            sb.append("| ");
            sb.append(v.severity());
            sb.append(" | ");
            sb.append("[" + v.cveId()).append("](").append("https://www.cve.org/CVERecord?id=").append(v.cveId()).append(")");
            sb.append(" | ");
            sb.append(v.packageName());
            sb.append(" | ");
            sb.append(v.affectedStreams().stream().sorted().collect(Collectors.joining(", ")));
            sb.append(" | ");
            sb.append(v.installedVersions().stream().sorted().collect(Collectors.joining(", ")));
            sb.append(" | ");
            sb.append(v.fixedVersions().stream().sorted().collect(Collectors.joining(", ")));
            sb.append(" | ");
            if (gitHubIssues != null) {
                sb.append(gitHubIssues.getIssues(v).stream().map(i -> "[#" + i.number() + "](" + i.htmlUrl() + ")").collect(Collectors.joining(" ")));
            } else {
                sb.append(" ");
            }
            sb.append(" | ");
            sb.append(v.tools().stream().sorted().map(t -> t.name().toLowerCase()).collect(Collectors.joining(", ")));
            sb.append(" |\n");
        });

        if (gitHubIssues != null) {
            sb.append("\n");

            sb.append("## Issues without open vulnerabilities\n\n");
            sb.append("| Issue | Title | State | Labels |\n");
            sb.append("| ----- | ----- | ------ | ------ |\n");
            gitHubIssues.getUnmatchedIssues().forEach(i -> {
                sb.append("| ");
                sb.append("[#").append(i.number()).append("](").append(i.htmlUrl()).append(")");
                sb.append(" | ");
                sb.append(i.title());
                sb.append(" | ");
                sb.append(i.state());
                sb.append(" | ");
                sb.append(String.join(", ", i.labels()));
                sb.append(" |\n");
            });
        }

        return sb.toString();
    }

}
