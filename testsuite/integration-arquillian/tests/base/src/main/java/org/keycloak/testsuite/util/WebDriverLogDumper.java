package org.keycloak.testsuite.util;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;

/**
 * Created by st on 21/03/17.
 */
public class WebDriverLogDumper {

    public static String dumpBrowserLogs(WebDriver driver) {
        try {
            StringBuilder sb = new StringBuilder();
            LogEntries logEntries = driver.manage().logs().get("browser");
            for (LogEntry e : logEntries.getAll()) {
                sb.append("\n\t" + e.getMessage());
            }
            return sb.toString();
        } catch (UnsupportedOperationException e) {
            return "Browser doesn't support fetching logs";
        }
    }

}
