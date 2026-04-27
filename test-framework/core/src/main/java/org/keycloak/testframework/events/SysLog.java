package org.keycloak.testframework.events;

import java.time.Instant;
import java.util.Date;

public class SysLog {

    private static final String SEPARATOR = " - \uFEFF";

    private Date timestamp;
    private String hostname;
    private String appName;
    private String category;
    private String message;

    private SysLog() {
    }

    public static SysLog parse(String logEntry) {
        int i = logEntry.indexOf(SEPARATOR);

        String[] header = logEntry.substring(0, i).split(" ");

        SysLog sysLog = new SysLog();
        sysLog.timestamp = Date.from(Instant.parse(header[1]));
        sysLog.hostname = header[2];
        sysLog.appName = header[3];
        sysLog.category = header[5];
        sysLog.message = logEntry.substring(i + SEPARATOR.length());
        return sysLog;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getHostname() {
        return hostname;
    }

    public String getAppName() {
        return appName;
    }

    public String getCategory() {
        return category;
    }

    public String getMessage() {
        return message;
    }
}
