package org.keycloak.testsuite.arquillian.jetty.container;

import org.eclipse.jetty.util.Jetty;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum AppServerVersion {
    INSTANCE;

    private String appServerVersion;

    AppServerVersion() {
        Pattern versionExtraction = Pattern.compile("(\\d\\.\\d).*");
        Matcher m = versionExtraction.matcher(Jetty.VERSION);
        if (!m.find()) {
            throw new IllegalStateException("Could not parse Jetty version: " + Jetty.VERSION);
        }
        appServerVersion = m.group(1).replaceAll("\\.", "");
    }

    public String getAppServerVersion() {
        return appServerVersion;
    }
}
