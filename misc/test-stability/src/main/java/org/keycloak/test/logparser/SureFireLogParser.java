package org.keycloak.test.logparser;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SureFireLogParser implements LogParser {

    private static final Pattern FAILURE = Pattern.compile("([^\\t]*)\\t.*\\[ERROR] {3}(.*Test[^ ]*).*");

    @Override
    public boolean supports(List<String> lines) {
        return lines.stream().anyMatch(l -> l.contains("org.apache.maven.plugins:maven-surefire-plugin"));
    }

    public List<LogFailure> parseFailures(List<String> lines) {
        List<LogFailure> logFailures = new LinkedList<>();
        for (String l : lines) {
            Matcher m = FAILURE.matcher(l);
            if (m.matches()) {
                logFailures.add(new LogFailure(m.group(1), m.group(2)));
            }
        }
        return logFailures;
    }

}
