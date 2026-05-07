package org.keycloak.test.migration;

public class WebDriverRewrite extends TestRewrite {

    @Override
    public void rewrite() {
        int droneLine = findLine("import org\\.jboss\\.arquillian\\.drone\\.api\\.annotation\\.Drone;");
        int driverLine = findLine(".*WebDriver driver;.*");
        int driverRef = findLine(".*driver\\..*");

        if (droneLine >= 0) {
            if (driverLine >= 0) {
                String current = content.get(droneLine);
                String migrateTo = "import org.keycloak.testframework.ui.annotations.InjectWebDriver;";
                replaceLine(droneLine, migrateTo);
                info(droneLine, "Import rewritten: '" + current + "' --> '" + migrateTo + "'");
                addImport("org.keycloak.testframework.ui.webdriver.ManagedWebDriver");
            } else {
                addManagedWebDriver();
            }

            for (int i = 0; i < content.size(); i++) {
                String n = content.get(i);
                if (n.trim().equals("@Drone")) {
                    content.remove(i);
                    if (content.get(i).trim().equals("@SecondBrowser")) {
                        content.add(i, n.replace("@Drone", "@InjectWebDriver(ref = \"secondDriver\")"))     ;
                        content.remove(i + 1);
                    } else if (content.get(i).trim().equals("@ThirdBrowser")){
                        content.add(i, n.replace("@Drone", "@InjectWebDriver(ref = \"thirdDriver\")"))     ;
                        content.remove(i + 1);
                    } else {
                        content.add(i, n.replace("@Drone", "@InjectWebDriver"));
                    }
                    String driver = content.get(i + 1);
                    content.remove(i + 1);
                    content.add(i + 1, driver.replaceFirst("WebDriver", "ManagedWebDriver"));
                    info(i, "@Drone rewritten to @InjectWebDriver");
                }
            }
        } else if (driverRef >= 0) {
            addManagedWebDriver();
        }
    }

    private void addManagedWebDriver() {
        addImport("org.keycloak.testframework.ui.annotations.InjectWebDriver");
        addImport("org.keycloak.testframework.ui.webdriver.ManagedWebDriver");

        int managedRealm = findLine("    ManagedRealm managedRealm;");

        content.add(managedRealm + 1, "");
        content.add(managedRealm + 2, "    @InjectWebDriver");
        content.add(managedRealm + 3, "    ManagedWebDriver driver;");

        info(managedRealm + 2, "Injecting: WebDriver");
    }

}
