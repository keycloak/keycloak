package org.keycloak.misc.vulns.github;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.keycloak.misc.vulns.Vuln;
import org.keycloak.misc.vulns.cveorg.CveInfo;
import org.keycloak.misc.vulns.cveorg.CveOrg;
import org.keycloak.misc.vulns.utils.FindDependency;

public class GitHubIssues {

    private final GitHubApi gitHubApi;
    private final boolean updateIssues;
    private final List<GitHubIssue> allIssues;
    private final Map<String, List<GitHubIssue>> cveToIssues = new HashMap<>();
    private final Set<String> backportLabels;

    public GitHubIssues(GitHubApi gitHubApi, boolean updateIssues) throws GitHubApiException {
        backportLabels = gitHubApi.searchLabels("backport/");
        this.gitHubApi = gitHubApi;
        this.updateIssues = updateIssues;

        String query = "(repo:" + gitHubApi.getRepository() + " label:area/dependencies label:kind/cve) AND ((is:open) OR (is:closed label:" + String.join(",", backportLabels) + "))";
        allIssues = new LinkedList<>(gitHubApi.searchIssues(query));
        System.out.println("Found " + allIssues.size() + " issues");
    }

    public void matchIssues(List<Vuln> vulns) throws IOException, InterruptedException, GitHubApiException {
        for (Vuln vuln : vulns) {
            List<GitHubIssue> vulnIssues = allIssues.stream().filter(i -> i.title().equals(toTitle(vuln))).toList();

            if (updateIssues) {
                if (vulnIssues.isEmpty()) {
                    GitHubIssue issue = createIssue(vuln);
                    allIssues.add(issue);
                    System.out.println("* Created issue " + issue.htmlUrl() + " " + issue.title());
                } else {
                    GitHubIssue managedIssue = vulnIssues.stream().filter(i -> i.labels().contains("source/scan-dependencies")).findFirst().orElse(null);
                    if (managedIssue != null) {
                        GitHubIssue updatedIssue = updateIssue(managedIssue, vuln);
                        if (updatedIssue != null) {
                            allIssues.remove(managedIssue);
                            allIssues.add(updatedIssue);
                            System.out.println("* Updated issue " + updatedIssue.htmlUrl() + " " + updatedIssue.title());
                        }
                    }
                }
            }

            cveToIssues.put(vuln.cveId(), vulnIssues);
        }
    }

    public List<GitHubIssue> getIssues(Vuln vuln) {
        return cveToIssues.get(vuln.cveId());
    }

    public List<GitHubIssue> getUnmatchedIssues() {
        List<GitHubIssue> matchedIssues = cveToIssues.values().stream().flatMap(Collection::stream).toList();
        return allIssues.stream().filter(i -> !matchedIssues.contains(i)).toList();
    }

    private GitHubIssue updateIssue(GitHubIssue issue, Vuln vuln) throws GitHubApiException {
        GitHubIssueBuilder builder = gitHubApi.updateIssue(issue);
        boolean update = false;
        if (vuln.affectedStreams().contains("main")) {
            if (!"open".equals(issue.state())) {
                builder.reopen();
                update = true;
            }
        } else if ("closed".equals(issue.state()) && !"completed".equals(issue.stateReason())) {
            builder.reopen();
            update = true;
        }

        List<String> backportLabels = getBackportLabels(vuln);
        if (!issue.labels().containsAll(backportLabels)) {
            builder.label(backportLabels);
            update = true;
        }

        return update ? builder.send() : null;
    }

    private GitHubIssue createIssue(Vuln vuln) throws IOException, InterruptedException, GitHubApiException {
        CveInfo cveInfo = CveOrg.query(vuln.cveId());

        String title = toTitle(vuln);

        StringBuilder body = new StringBuilder();

        body.append("* **Severity:** ").append(vuln.severity().toLowerCase()).append('\n');
        body.append("* **CVE ID**: ").append(toCveLink(vuln.cveId())).append('\n');
        body.append("* **CVE Status**: ").append(cveInfo.cveMetadata().state()).append('\n');
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

        CveInfo.Description description = cveInfo.containers().cna().descriptions() != null ? cveInfo.containers().cna().descriptions().stream().filter(d -> d.lang().equals("en")).findFirst().orElse(null) : null;
        if (description != null) {
            body.append('\n');
            body.append("# Description\n");
            body.append(description.value());
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

        return gitHubApi.createIssue(title).body(body.toString()).type("cve").label(labels).send();
    }

    private static List<String> getBackportLabels(Vuln vuln) {
        return vuln.affectedStreams().stream()
                .filter(Objects::nonNull)
                .filter(a -> a.startsWith("release/"))
                .map(a -> a.replace("release/", "backport/"))
                .toList();
    }

    private static String toTitle(Vuln vuln) {
        return vuln.cveId() + " " + vuln.packageName();
    }

    private static String toCveLink(String cveId) {
        return "[" + cveId + "](https://www.cve.org/CVERecord?id=" + cveId + ")";
    }

    private static String toString(Set<String> strings) {
        return String.join(", ", strings);
    }

}
