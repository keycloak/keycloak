package org.keycloak.misc.vulns;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.misc.vulns.snyk.SnykReport;
import org.keycloak.misc.vulns.trivy.TrivyReport;

public class ReportParser {

    public static List<Vuln> parse(String stream, File file) {
        try {
            Tool reportType = detectType(file);
            return switch (reportType) {
                case SNYK -> parseSnyk(stream, file);
                case TRIVY -> parseTrivy(stream, file);
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Tool detectType(File file) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            for (String l = br.readLine(); l != null; l = br.readLine()) {
                if (l.contains("\"Trivy\"")) {
                    return Tool.TRIVY;
                } else if (l.contains("SNYK")) {
                    return Tool.SNYK;
                }
            }
        }
        throw new RuntimeException("Unknown report type");
    }

    private static List<Vuln> parseSnyk(String stream, File file) throws IOException {
        SnykReport snykReport = JsonUtil.read(file, SnykReport.class);
        return snykReport.vulnerabilities().stream()
                .map(v -> new Vuln(v.identifiers().get("CVE").get(0), v.severity().toLowerCase(), v.title(), v.packageName(), v.packageManager(), asSet(v.version()), asSet(v.fixedIn()), asSet(v.from()), asSet(stream), new HashSet<>(), asSet(Tool.SNYK)))
                .toList();
    }

    private static List<Vuln> parseTrivy(String stream, File file) throws IOException {
        List<Vuln> vulns = new LinkedList<>();
        TrivyReport trivyReport = JsonUtil.read(file, TrivyReport.class);
        for (TrivyReport.Result r : trivyReport.results()) {
            if (r.vulnerabilities() != null) {
                for (TrivyReport.Vulnerability tv : r.vulnerabilities()) {
                    vulns.add(new Vuln(tv.vulnerabilityId(), tv.severity().toLowerCase(), tv.title(), tv.pkgName(), tv.pkgIdentifier().pUrl().split("/")[0].substring(4), asSet(tv.installedVersion()), parseCommaSeparatedString(tv.fixedVersion()), asSet(r.target()), asSet(stream), new HashSet<>(), asSet(Tool.TRIVY)));
                }
            }
        }
        return dedupe(vulns);
    }

    public static List<Vuln> dedupe(List<Vuln> vulns) {
        List<Vuln> deduped = new LinkedList<>();
        for (Vuln v : vulns) {
            Optional<Vuln> existing = deduped.stream().filter(d -> d.cveId().equals(v.cveId())).findFirst();
            if (existing.isPresent()) {
                Vuln e = existing.get();
                e.affectedStreams().addAll(v.affectedStreams());
                e.installedVersions().addAll(v.installedVersions());
                e.fixedVersions().addAll(v.fixedVersions());
                e.tools().addAll(v.tools());
            } else {
                deduped.add(v);
            }
        }
        return deduped;
    }

    private static Set<String> parseCommaSeparatedString(String string) {
        return string != null ? Arrays.stream(string.split(",")).map(String::trim).collect(Collectors.toSet()) : Collections.emptySet();
    }

    private static Set<String> asSet(String string) {
        Set<String> s = new HashSet<>();
        s.add(string);
        return s;
    }

    private static Set<Tool> asSet(Tool tool) {
        Set<Tool> s = new HashSet<>();
        s.add(tool);
        return s;
    }

    private static Set<String> asSet(Collection<String> strings) {
        return new HashSet<>(strings);
    }

}
