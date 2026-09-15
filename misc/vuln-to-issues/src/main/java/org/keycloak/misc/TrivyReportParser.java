package org.keycloak.misc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.keycloak.misc.Vulnerability.AffectsRef;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueBuilder;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHIssueStateReason;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

public class TrivyReportParser {

    private static final String GH_TOKEN = System.getenv("GH_TOKEN");
    private static final String GITHUB_REPOSITORY = System.getenv("GITHUB_REPOSITORY");
    private static final String GITHUB_STEP_SUMMARY = System.getenv("GITHUB_STEP_SUMMARY");

    public static void main(String[] args) throws IOException {
        GitHub github = new GitHubBuilder().withJwtToken(GH_TOKEN).build();

        File runDir = new File("/tmp/trivy-logs");

        Map<String, Vulnerability> vulnerabilities = parseTrivyReports(runDir);
        updateGitHubIssues(github, vulnerabilities);
        createReport(vulnerabilities);
    }

    private static Map<String, Vulnerability> parseTrivyReports(File runDir) throws IOException {
        Map<String, Vulnerability> vulnerabilities = new HashMap<>();
        File[] refReportsDir = runDir.listFiles(File::isDirectory);

        for (File refReportDir : refReportsDir) {
            String refName = getRefNameFromReportDir(refReportDir.getName());

            ObjectMapper objectMapper = new ObjectMapper();
            TrivyReport trivyReport = objectMapper.reader().readValue(new File(refReportDir, "trivy-report.json"), TrivyReport.class);

            for (TrivyReport.Result result : trivyReport.results()) {
                if (result.vulnerabilities() != null && !result.vulnerabilities().isEmpty()) {
                    for (TrivyReport.Vulnerability v : result.vulnerabilities()) {
                        Vulnerability vulnerability = vulnerabilities.computeIfAbsent(v.vulnerabilityId(), s -> new Vulnerability(s, v.severity(), v.pkgName()));

                        AffectsRef affectsRef = vulnerability.getAffectsRefs().computeIfAbsent(refName, s -> new AffectsRef(s, v.installedVersion(), v.fixedVersion()));
                        affectsRef.getTargets().add(result.target());
                    }
                }
            }
        }
        return vulnerabilities;
    }

    private static void updateGitHubIssues(GitHub github, Map<String, Vulnerability> vulnerabilities) throws IOException {
        for (Vulnerability v : vulnerabilities.values()) {
            String[] repositorySplit = GITHUB_REPOSITORY.split("/");
            List<GHIssue> searchIssues = github.searchIssues().repo(repositorySplit[0], repositorySplit[1]).isIssue().q(v.getId() + " " + v.getPackageName()).list().toList();
            List<GHIssue> issues = new LinkedList<>(searchIssues);

            Optional<GHIssue> existingIssue = searchIssues.stream().filter(i -> i.getLabels().stream().anyMatch(l -> l.getName().equals("source/scan-dependencies"))).findFirst();
            if (existingIssue.isPresent()) {
                GHIssue issue = existingIssue.get();

                if (v.getAffectsRefs().containsKey("main")) {
                    if (!GHIssueState.OPEN.equals(issue.getState())) {
                        issue.reopen();
                    }
                } else if (GHIssueState.CLOSED.equals(issue.getState()) && !GHIssueStateReason.COMPLETED.equals(issue.getStateReason())) {
                    issue.reopen();
                }

                List<String> backportLabels = getBackportLabels(v);
                List<String> missingBackportLabels = backportLabels.stream().filter(bl -> issue.getLabels().stream().noneMatch(l -> l.getName().equals(bl))).toList();
                if (!missingBackportLabels.isEmpty()) {
                    issue.addLabels(missingBackportLabels.toArray(String[]::new));
                }

                issues.remove(issue);
                issues.add(0, issue);
            } else {
                issues.add(0, createIssue(github, v));
            }

            v.setIssues(issues);
        }
    }

    private static GHIssue createIssue(GitHub gitHub, Vulnerability vulnerability) throws IOException {
        String title = vulnerability.getId() + " " + vulnerability.getPackageName();

        StringBuilder body = new StringBuilder();

        body.append("* **CVE ID**: [").append(vulnerability.getId()).append("](").append("https://www.cve.org/CVERecord?id=").append(vulnerability.getId()).append(")\n");
        body.append("* **Package:** ").append(vulnerability.getPackageName()).append('\n');
        body.append("* **Severity:** ").append(vulnerability.getSeverity().toLowerCase()).append('\n');
        body.append("* **Affects:** ").append(vulnerability.getAffectsRefs().keySet().stream().collect(Collectors.joining(", "))).append('\n');
        body.append("* **Installed versions:** ").append(vulnerability.getAffectsRefs().values().stream().map(Vulnerability.AffectsRef::getInstalledVersion).distinct().collect(Collectors.joining(", "))).append('\n');
        body.append("* **Fixed versions:** ").append(vulnerability.getAffectsRefs().values().stream().map(Vulnerability.AffectsRef::getFixedVersions).distinct().collect(Collectors.joining(", ")));
        body.append('\n');

        body.append("# Affected modules").append('\n');

        for (Vulnerability.AffectsRef affectsRef : vulnerability.getAffectsRefs().values()) {
            body.append("## ").append(affectsRef.getRefName()).append('\n');
            body.append("```\n");
            affectsRef.getTargets().forEach(t -> body.append(t).append('\n'));
            body.append("```\n");
        }

        GHIssueBuilder issueBuilder = gitHub.getRepository(GITHUB_REPOSITORY).createIssue(title).body(body.toString());

        issueBuilder.label("status/triage").label("source/scan-dependencies").label("area/dependencies").label("kind/cve").label("severity/" + vulnerability.getSeverity().toLowerCase());

        getBackportLabels(vulnerability).forEach(issueBuilder::label);

        return issueBuilder.create();
    }

    private static List<String> getBackportLabels(Vulnerability vulnerability) {
        return vulnerability.getAffectsRefs().keySet().stream()
                .filter(a -> a.startsWith("release/"))
                .map(a -> a.replace("release/", "backport/"))
                .toList();
    }

    private static void createReport(Map<String, Vulnerability> vulnerabilities) {
        StringBuilder sb = new StringBuilder();

        sb.append("| CVE ID | Package | Severity | Affects | Installed versions | Fixed versions | GitHub Issues |").append('\n');
        sb.append("| ------ | ------- | -------- | ------- | ------------------ | -------------- | ------------- |").append('\n');

        for (Vulnerability v : vulnerabilities.values()) {
            sb.append("| ");
            sb.append("[" + v.getId()).append("](").append("https://www.cve.org/CVERecord?id=").append(v.getId()).append(")");
            sb.append(" | ");
            sb.append(v.getPackageName());
            sb.append(" | ");
            sb.append(v.getSeverity().toLowerCase());
            sb.append(" | ");
            sb.append(v.getAffectsRefs().keySet().stream().collect(Collectors.joining(", ")));
            sb.append(" | ");
            sb.append(v.getAffectsRefs().values().stream().map(AffectsRef::getInstalledVersion).distinct().collect(Collectors.joining(", ")));
            sb.append(" | ");
            sb.append(v.getAffectsRefs().values().stream().map(AffectsRef::getFixedVersions).distinct().collect(Collectors.joining(", ")));
            sb.append(" | ");
            sb.append(v.getIssues().stream().map(i -> "[" + i.getNumber() + "](" + i.getHtmlUrl() + ")").collect(Collectors.joining(" ")));
            sb.append(" |\n");
        }

        try {
            PrintWriter pw = new PrintWriter(new FileWriter(GITHUB_STEP_SUMMARY, true));
            pw.print(sb);
            pw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getRefNameFromReportDir(String reportDirName) {
        return reportDirName.replace("trivy-report-", "")
                .replace('_', '/')
                .replace("refs/heads/", "")
                .replace("refs/tags/", "tag/");
    }

}
