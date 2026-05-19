package org.keycloak.testframework.log;

import java.util.stream.Stream;

public class Logs {

    /**
     * Returns log entries from the managed Keycloak server that happened during test execution
     * @return log entries from the managed Keycloak server
     */
    public Stream<LogEntry> getManagedKeycloakLogs() {
        return LogQueue.getInstance().snapshot().stream()
                .filter(l -> l.getState().equals(LogQueue.State.RUNNING))
                .filter(LogEntry::isManagedKeycloakLogEntry);
    }

}
