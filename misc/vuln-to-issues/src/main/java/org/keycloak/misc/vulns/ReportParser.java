package org.keycloak.misc.vulns;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonFactory;

import com.fasterxml.jackson.core.JsonParser;

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
        JsonFactory factory = new JsonFactory();
        try (JsonParser parser = factory.createParser(file)) {
            while (parser.nextToken() != null) {
                String fieldName = parser.currentName();
                if ("Trivy".equals(fieldName)) {
                    return Tool.TRIVY;
                }
                if ("vulnerabilities".equals(fieldName)) {
                    return Tool.SNYK;
                }
            }
        }
        throw new RuntimeException("Unknown report type");
    }

    private static List<Vuln> parseSnyk(String stream, File file) throws IOException {
        List<Vuln> vulns = new LinkedList<>();
        SnykReport snykReport = JsonUtil.read(file, SnykReport.class);
        if (snykReport.vulnerabilities() != null) {
            for (SnykReport.SnykVulnerability sv : snykReport.vulnerabilities()) {
                if ("license".equals(sv.type())) {
                    continue;
                }
                if (sv.identifiers().containsKey("CVE") && !sv.identifiers().get("CVE").isEmpty()) {
                    for (String cveId : sv.identifiers().get("CVE")) {
                        vulns.add(new Vuln(cveId, Severity.of(sv.severity()), sv.title(), sv.packageName(), sv.packageManager(), asSet(sv.version()), asSet(sv.fixedIn()), asSet(sv.from()), asSet(stream), asSet(Tool.SNYK)));
                    }
                } else {
                    vulns.add(new Vuln(sv.id(), Severity.of(sv.severity()), sv.title(), sv.packageName(), sv.packageManager(), asSet(sv.version()), asSet(sv.fixedIn()), asSet(sv.from()), asSet(stream), asSet(Tool.SNYK)));
                }
            }
        }
        return dedupe(vulns);
    }

    private static List<Vuln> parseTrivy(String stream, File file) throws IOException {
        List<Vuln> vulns = new LinkedList<>();
        TrivyReport trivyReport = JsonUtil.read(file, TrivyReport.class);
        for (TrivyReport.Result r : trivyReport.results()) {
            if (r.vulnerabilities() != null) {
                for (TrivyReport.Vulnerability tv : r.vulnerabilities()) {
                    vulns.add(new Vuln(tv.vulnerabilityId(), Severity.of(tv.severity()), tv.title(), tv.pkgName(), tv.pkgIdentifier().pUrl().split("/")[0].substring(4), asSet(tv.installedVersion()), parseCommaSeparatedString(tv.fixedVersion()), asSet(r.target()), asSet(stream), asSet(Tool.TRIVY)));
                }
            }
        }
        return dedupe(vulns);
    }

    public static List<Vuln> dedupe(List<Vuln> vulns) {
        List<Vuln> deduped = new LinkedList<>();
        for (Vuln v : vulns) {
            Optional<Vuln> existing = deduped.stream().filter(d -> d.id().equals(v.id()) && d.packageName().equals(v.packageName())).findFirst();
            if (existing.isPresent()) {
                Vuln e = existing.get();
                if (e.severity().compareTo(v.severity()) > 0) {
                    e.setSeverity(v.severity());
                }
                e.affectedStreams().addAll(v.affectedStreams());
                e.installedVersions().addAll(v.installedVersions());
                e.fixedVersions().addAll(v.fixedVersions());
                e.targets().addAll(v.targets());
                e.tools().addAll(v.tools());
            } else {
                deduped.add(v);
            }
        }
        return deduped;
    }

    private static Set<String> parseCommaSeparatedString(String string) {
        return string != null ? Arrays.stream(string.split(",")).map(String::trim).collect(Collectors.toSet()) : new HashSet<>();
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
