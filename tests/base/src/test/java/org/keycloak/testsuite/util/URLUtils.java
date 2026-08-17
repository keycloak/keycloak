package org.keycloak.testsuite.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

public final class URLUtils {

    private URLUtils() {
    }

    public static String getActionUrlFromCurrentPage(ManagedWebDriver driver) {
        return getActionUrlFromCurrentPage(driver.driver());
    }

    public static String getActionUrlFromCurrentPage(WebDriver driver) {
        Matcher m = Pattern.compile("form action=\"([^\"]*)\"").matcher(driver.getPageSource());
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    public static void sendPOSTRequestWithWebDriver(ManagedWebDriver driver, String postRequestUrl, Map<String, String> formParams) {
        sendPOSTRequestWithWebDriver(driver.driver(), postRequestUrl, formParams);
    }

    public static void sendPOSTRequestWithWebDriver(String postRequestUrl, Map<String, String> formParams) {
        sendPOSTRequestWithWebDriver(DroneUtils.getCurrentDriver(), postRequestUrl, formParams);
    }

    public static void sendPOSTRequestWithWebDriver(WebDriver driver, String postRequestUrl, Map<String, String> formParams) {
        StringBuilder script = new StringBuilder();
        script.append("var f=document.createElement('form');")
                .append("f.method='POST';")
                .append("f.action='").append(postRequestUrl.replace("'", "\\'")).append("';");
        for (Map.Entry<String, String> e : formParams.entrySet()) {
            script.append("var i=document.createElement('input');")
                    .append("i.type='hidden';")
                    .append("i.name='").append(e.getKey().replace("'", "\\'")).append("';")
                    .append("i.value='").append(e.getValue().replace("'", "\\'")).append("';")
                    .append("f.appendChild(i);");
        }
        script.append("document.body.appendChild(f);f.submit();");
        ((JavascriptExecutor) driver).executeScript(script.toString());
    }
}
