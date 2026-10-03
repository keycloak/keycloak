package org.keycloak.misc.vulns;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.keycloak.misc.vulns.github.GitHubApi;
import org.keycloak.misc.vulns.github.GitHubApiException;
import org.keycloak.misc.vulns.github.GitHubIssues;

public class ScanDependenciesReport {

    public static void main(String[] args) throws IOException, InterruptedException, GitHubApiException {
        Config.init(args);

        List<Vuln> vulns = parseReports(Config.REPORTS);
        filterIgnored(Config.IGNORE, vulns);

        GitHubIssues issues = matchIssues(vulns);
        createReport(vulns, issues);
    }

    private static List<Vuln> parseReports(File reportArg) {
        printHeader("Parsing reports");

        List<Vuln> vulns = new LinkedList<>();

        if (reportArg.isFile()) {
            System.out.println("Parsing report: " + reportArg);
            vulns.addAll(ReportParser.parse(null, reportArg));
        } else if (reportArg.isDirectory()) {
            File[] childDirs = reportArg.listFiles(File::isDirectory);
            boolean multipleRefs = childDirs != null && Arrays.stream(childDirs).anyMatch(f -> f.getName().contains("refs_heads"));
            if (multipleRefs) {
                System.out.println("Parsing reports from multiple streams: ");
                List<Vuln> allVulns = new LinkedList<>();
                for (File refDir : childDirs) {
                    String stream = refDir.getName().substring("reports-refs_heads_".length()).replace('_', '/');
                    File[] reports = refDir.listFiles(File::isFile);
                    if (reports != null) {
                        Arrays.stream(reports).forEach(report -> {
                            System.out.println(" - " + report);
                            allVulns.addAll(ReportParser.parse(stream, report));
                        });
                    }
                }
                vulns = ReportParser.dedupe(allVulns);
            } else {
                File[] reports = reportArg.listFiles(File::isFile);
                if (reports != null) {
                    List<Vuln> allVulns = new LinkedList<>();
                    System.out.println("Parsing multiple reports:");
                    Arrays.stream(reports).forEach(report -> {
                        System.out.println(" - " + report);
                        allVulns.addAll(ReportParser.parse(null, report));
                    });
                    vulns = ReportParser.dedupe(allVulns);
                }
            }
        }

        System.out.println();
        System.out.println("Found " + vulns.size() + " vulnerabilities");

        return vulns;
    }

    private static void filterIgnored(File ignoreFile, List<Vuln> vulns) throws IOException {
        printHeader("Filter ignored CVEs");

        CveIgnore allIgnored = null;
        if (ignoreFile != null) {
            if (ignoreFile.isFile()) {
                allIgnored = YamlUtil.read(ignoreFile, CveIgnore.class);
            } else {
                System.out.println("Ignore file " + ignoreFile + " not found");
            }
        }

        if (allIgnored == null) {
            allIgnored = new CveIgnore(Collections.emptyList());
        }

        ListIterator<Vuln> itr = vulns.listIterator();
        Set<String> ignoredKeycloakCves = new HashSet<>();
        while (itr.hasNext()) {
            Vuln vuln = itr.next();
            if (vuln.packageName().startsWith("org.keycloak")) {
                itr.remove();
                ignoredKeycloakCves.add(vuln.cveId());
            } else {
                CveIgnore.Ignored ignored = allIgnored.vulnerabilities().stream().filter(i -> i.cveId().equals(vuln.cveId())).findFirst().orElse(null);
                if (ignored != null) {
                    itr.remove();
                    System.out.println("* Ignored: " + vuln.cveId() + ", statement: " + ignored.statement() + ", issue: " + ignored.gitHubIssue());
                }
            }
        }
        if (!ignoredKeycloakCves.isEmpty()) {
            System.out.println("* Ignored " + ignoredKeycloakCves.size() + " CVEs in Keycloak (" + String.join(", ", ignoredKeycloakCves) + ")");
        }
    }

    private static GitHubIssues matchIssues(List<Vuln> vulns) throws GitHubApiException, IOException, InterruptedException {
        if (Config.REPOSITORY == null) {
            return null;
        }

        printHeader("Match GitHub issues");

        GitHubApi gitHubApi = new GitHubApi(Config.REPOSITORY);
        GitHubIssues issues = new GitHubIssues(gitHubApi, Config.UPDATE_ISSUES);
        issues.matchIssues(vulns);
        return issues;
    }

    private static void createReport(List<Vuln> vulns, GitHubIssues issues) throws IOException {
        printHeader("Report");

        String reportSummary = Report.createReport(vulns, issues);
        System.out.println(reportSummary);
        if (Config.OUTPUT != null) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(Config.OUTPUT, true))) {
                pw.print(reportSummary);
            }
        }
    }

    private static void printHeader(String step) {
        System.out.println("================================================================================");
        System.out.println(step);
        System.out.println("--------------------------------------------------------------------------------");
    }

}
