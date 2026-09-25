package org.keycloak.misc.vulns.github;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.keycloak.misc.vulns.Vuln;
import org.keycloak.misc.vulns.cveorg.CveInfo;
import org.keycloak.misc.vulns.cveorg.CveOrg;
import org.keycloak.misc.vulns.utils.FindDependency;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueBuilder;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHIssueStateReason;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

public class GitHubIssues {

    public static void updateGitHubIssues(String ghToken, GitHub github, GHRepository repository, List<Vuln> vulns, boolean updateIssues) throws IOException, URISyntaxException, InterruptedException {
        for (Vuln v : vulns) {
            System.out.print(".");

            List<GHIssue> searchIssues = github.searchIssues().repo(repository.getOwnerName(), repository.getName()).isIssue().q(v.cveId() + " " + v.packageName()).list().toList();
            List<GHIssue> issues = new LinkedList<>(searchIssues);

            if (updateIssues) {
                Optional<GHIssue> existingIssue = searchIssues.stream().filter(i -> i.getLabels().stream().anyMatch(l -> l.getName().equals("source/scan-dependencies"))).findFirst();
                if (existingIssue.isPresent()) {
                    GHIssue issue = existingIssue.get();
                    updateIssue(issue, v);

                    issues.remove(issue);
                    issues.add(0, issue);
                } else {
                    issues.add(0, createIssue(ghToken, repository, v));
                }
            }

            v.issues().clear();
            v.issues().addAll(issues);
        }
        System.out.println();
    }

    private static void updateIssue(GHIssue issue, Vuln vuln) throws IOException {
        if (vuln.affectedStreams().contains("main")) {
            if (!GHIssueState.OPEN.equals(issue.getState())) {
                issue.reopen();
            }
        } else if (GHIssueState.CLOSED.equals(issue.getState()) && !GHIssueStateReason.COMPLETED.equals(issue.getStateReason())) {
            issue.reopen();
        }

        List<String> backportLabels = getBackportLabels(vuln);
        List<String> missingBackportLabels = backportLabels.stream().filter(bl -> issue.getLabels().stream().noneMatch(l -> l.getName().equals(bl))).toList();
        if (!missingBackportLabels.isEmpty()) {
            issue.addLabels(missingBackportLabels.toArray(String[]::new));
        }
    }

    private static GHIssue createIssue(String ghToken, GHRepository repository, Vuln vuln) throws IOException, URISyntaxException, InterruptedException {
        CveInfo cveInfo = CveOrg.query(vuln.cveId());

        String title = vuln.cveId() + " " + vuln.packageName();

        StringBuilder body = new StringBuilder();

        body.append("* **Severity:** ").append(vuln.severity().toLowerCase()).append('\n');
        body.append("* **CVE ID**: ").append(toCveLink(vuln.cveId())).append('\n');
        body.append("* **Package:** ").append(vuln.packageName()).append('\n');
        body.append("* **Title:** ").append(cveInfo.containers().cna().title()).append('\n');
        body.append("* **Published:** ").append(cveInfo.cveMetadata().datePublished()).append('\n');
        body.append("* **Affects:** ").append(toString(vuln.affectedStreams())).append('\n');
        body.append("* **Installed versions:** ").append(toString(vuln.installedVersions())).append('\n');
        body.append("* **Fixed versions:** ").append(toString(vuln.fixedVersions())).append('\n');
        body.append('\n');

        body.append("# Affected targets").append('\n');
        body.append("```\n");
        body.append(String.join("\n", vuln.targets()));
        body.append('\n');
        body.append("```\n");

        Optional<CveInfo.Description> description = cveInfo.containers().cna().descriptions().stream().filter(d -> d.lang().equals("en")).findFirst();
        if (description.isPresent()) {
            body.append('\n');
            body.append("# Description\n");
            body.append(description.get().value());
        }

        Set<String> labels = new HashSet<>();
        labels.add("status/triage");
        labels.add("source/scan-dependencies");
        labels.add("area/dependencies");
        labels.add("kind/cve");
        labels.add("severity/" + vuln.severity().toLowerCase());
        labels.addAll(getBackportLabels(vuln));

        if (vuln.packageManager().equals("maven")) {
            Set<String> declaredIn = FindDependency.findDependency(vuln.packageName());
            if (!declaredIn.isEmpty()) {
                body.append('\n');
                body.append("# Declared In\n");
                body.append("```\n");
                body.append(String.join("\n", declaredIn));
                body.append("\n```\n");

                if (declaredIn.stream().anyMatch(d -> d.contains("quarkus-bom"))) {
                    labels.add("area/dependencies/quarkus");
                }
            }
        } else if (vuln.packageManager().equals("npm")) {
            labels.add("area/dependencies/js");
        }

        GHIssueBuilder issueBuilder = repository.createIssue(title).body(body.toString());
        labels.forEach(issueBuilder::label);

        GHIssue issue = issueBuilder.create();

        if (repository.getOwner().getName().equals("keycloak")) {
            GitHubUtil.setIssueType(ghToken, issue, "cve");
        }

        return issue;
    }

    private static List<String> getBackportLabels(Vuln vuln) {
        return vuln.affectedStreams().stream()
                .filter(Objects::nonNull)
                .filter(a -> a.startsWith("release/"))
                .map(a -> a.replace("release/", "backport/"))
                .toList();
    }

    private static String toCveLink(String cveId) {
        return "[" + cveId + "](https://www.cve.org/CVERecord?id=" + cveId + ")";
    }

    private static String toString(Set<String> strings) {
        return String.join(", ", strings);
    }

}
