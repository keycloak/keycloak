package org.keycloak.test.logparser;

import java.util.List;

public interface LogParser {

    boolean supports(List<String> lines);

    List<LogFailure> parseFailures(List<String> lines);

}
