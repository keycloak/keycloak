package org.keycloak.test.migration;

public class WebDriverRewrite extends TestRewrite {

    @Override
    public void rewrite() {
        int webDriverLine = findLine("import org\\.jboss\\.arquillian\\.drone\\.api\\.annotation\\.Drone;");
        if (webDriverLine >= 0) {
            String current = content.get(webDriverLine);
            String migrateTo = "import org.keycloak.testframework.ui.annotations.InjectWebDriver;";
            replaceLine(webDriverLine, migrateTo);
            info(webDriverLine, "Import rewritten: '" + current + "' --> '" + migrateTo + "'");
            addImport("org.keycloak.testframework.ui.webdriver.ManagedWebDriver");
            removeLine("import org.openqa.selenium.WebDriver;");

            for (int i = 0; i < content.size(); i++) {
                String n = content.get(i);
                if (n.trim().equals("@Drone")) {
                    content.remove(i);
                    if (!content.get(i+1).trim().contains("WebDriver driver2")) {
                        content.add(i, n.replace("@Drone", "@InjectWebDriver"));
                        int managedDriverLine = findLine(".*[ ]WebDriver[ ]driver;.*");
                        replaceLine(managedDriverLine, "    ManagedWebDriver managedDriver;");
                    } else {
                        content.add(i, n.replace("@Drone", "@InjectWebDriver(ref = \"secondDriver\")"));
                        int managedDriverLine = findLine(".*[ ]WebDriver[ ]driver2;.*");
                        replaceLine(managedDriverLine, "    ManagedWebDriver managedDriver2;");
                    }
                    info(i, "@Drone rewritten to @InjectWebDriver");
                }
            }
        }
    }

}
