package org.keycloak.it.junit5.extension.approvalTests;

import org.lambda.functions.Function0;

import java.util.Locale;

public class WindowsOrUnixOsEnvironmentLabeller implements Function0<String> {

    private static final String WINDOWS_NAME = "windows";
    private static final String UNIX_NAME = "unix";

    @Override
    public String call()
    {
        String osName = System.getProperty("os.name");

        if(osName.toLowerCase(Locale.ROOT).contains(WINDOWS_NAME)) {
            return WINDOWS_NAME;
        }

        //unix suffices, as basically all other OSses use sh files
        return UNIX_NAME;
    }
}
