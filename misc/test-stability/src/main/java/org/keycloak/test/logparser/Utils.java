package org.keycloak.test.logparser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class Utils {

    public static List<String> readLines(File file) throws IOException {
        return Files.lines(file.toPath()).toList();
    }

    public static List<RunFailure> toRunFailures(List<GitHubRun> runs) {
        List<RunFailure> failedRuns = new LinkedList<>();
        for (GitHubRun run : runs) {
            for (GitHubRun.GitHubRunJob job : run.jobs()) {
                if (job.conclusion().equals("failure")) {
                    for (GitHubRun.GitHubRunJobStep step : job.steps()) {
                        if (step.conclusion().equals("failure")) {
                            String fullName = run.name() + " / " + job.name() + " / " + step.name();
                            RunFailure failedRun = failedRuns.stream().filter(f -> f.run().equals(fullName)).findFirst().or(() -> {
                                RunFailure fr = new RunFailure(fullName, new LinkedList<>());
                                failedRuns.add(fr);
                                return Optional.of(fr);
                            }).get();

                            failedRun.details().add(new RunFailure.FailedRunDetails(run.name(), run.databaseId(), job.name(), job.databaseId(), job.url()));
                        }
                    }
                }
            }
        }
        return failedRuns;
    }

}
