package org.keycloak.testframework.ui.webdriver;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DriverOptionsTest {

    @Test
    public void extraArgumentsFollowHeadlessArguments() {
        List<String> base = args(DriverOptions.createChromeOptions(true));
        List<String> withExtra = args(DriverOptions.createChromeOptions(true, "--lang=de"));

        assertEquals(base.size() + 1, withExtra.size());
        assertEquals(base, withExtra.subList(0, base.size()));
        assertEquals("--lang=de", withExtra.get(base.size()));
    }

    @Test
    public void noExtraArgumentsKeepsDefaults() {
        assertEquals(args(DriverOptions.createChromeOptions(false)), args(DriverOptions.createChromeOptions(false, new String[0])));
    }

    @SuppressWarnings("unchecked")
    private static List<String> args(ChromeOptions options) {
        return (List<String>) ((Map<String, Object>) options.asMap().get("goog:chromeOptions")).get("args");
    }
}
