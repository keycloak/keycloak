package org.keycloak.misc.vulns;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Report {

    public static String createReport(List<Vuln> vulns) {
        StringBuilder sb = new StringBuilder();

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
            sb.append(v.issues().stream().map(i -> "[" + i.getNumber() + "](" + i.getHtmlUrl() + ")").collect(Collectors.joining(" ")));
            sb.append(" | ");
            sb.append(v.tools().stream().sorted().map(t -> t.name().toLowerCase()).collect(Collectors.joining(", ")));
            sb.append(" |\n");
        });

        return sb.toString();
    }

}
