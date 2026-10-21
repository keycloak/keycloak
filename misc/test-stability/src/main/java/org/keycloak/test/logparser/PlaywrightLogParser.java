package org.keycloak.test.logparser;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaywrightLogParser implements LogParser {

    private static final Pattern FAILURE = Pattern.compile("([^\\t]*)\\t.*##\\[error].*› (test/[^›]*).*");

    @Override
    public boolean supports(List<String> lines) {
        return lines.stream().anyMatch(l -> l.contains("Playwright test"));
    }

    public List<LogFailure> parseFailures(List<String> lines) {
        List<LogFailure> logFailures = new LinkedList<>();
        for (String l : lines) {
            Matcher m = FAILURE.matcher(l);

            if (m.matches()) {
                String job = m.group(1);
                String test = m.group(2);

                if (logFailures.stream().noneMatch(lf -> lf.job().equals(job) && lf.test().equals(test))) {
                    logFailures.add(new LogFailure(job, test));
                }
            }
        }
        return logFailures;
    }

}
