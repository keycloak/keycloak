package org.keycloak.misc.vulns;

import org.keycloak.misc.vulns.cveorg.CveInfo;
import org.keycloak.misc.vulns.cveorg.CveOrg;
import org.keycloak.misc.vulns.github.GitHubIssues;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Main {

    private static final String GH_TOKEN = System.getenv("GH_TOKEN");
    private static final String GITHUB_REPOSITORY = System.getenv("GITHUB_REPOSITORY");
    private static final String GITHUB_STEP_SUMMARY = System.getenv("GITHUB_STEP_SUMMARY");

    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        if (args.length == 0) {
            throw new RuntimeException("Missing report(s) argument");
        }

        File reportArg = new File(args[0]);

        List<Vuln> vulns = new LinkedList<>();

        if (reportArg.isFile()) {
            System.out.println("Parsing single report: " + reportArg);
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

        if (GITHUB_REPOSITORY != null && GH_TOKEN != null) {
            System.out.println("Updating GitHub Issues");

            GitHub github = new GitHubBuilder().withJwtToken(GH_TOKEN).build();
            GHRepository repository = github.getRepository(GITHUB_REPOSITORY);
            GitHubIssues.updateGitHubIssues(GH_TOKEN, github, repository, vulns, true);
        } else if (GITHUB_REPOSITORY != null) {
            System.out.println("Finding GitHub Issues");

            GitHub github = new GitHubBuilder().build();
            GHRepository repository = github.getRepository(GITHUB_REPOSITORY);
            GitHubIssues.updateGitHubIssues(GH_TOKEN, github, repository, vulns, false);
        }

        String reportSummary = Report.createReport(vulns);
        System.out.println(reportSummary);
        if (GITHUB_STEP_SUMMARY != null) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(GITHUB_STEP_SUMMARY, true))) {
                pw.print(reportSummary);
            }
        }
    }

}
