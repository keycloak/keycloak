package org.keycloak.testsuite.pages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;

import org.keycloak.services.Urls;
import org.keycloak.testsuite.Constants;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AccountApplicationsPage extends AbstractAccountPage {

    private String path = Urls.accountApplicationsPage(UriBuilder.fromUri(Constants.AUTH_SERVER_ROOT).build(), "test").toString();

    @Override
    public boolean isCurrent() {
        return driver.getTitle().contains("Account Management") && driver.getCurrentUrl().endsWith("/account/applications");
    }

    @Override
    public void open() {
        driver.navigate().to(path);
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void revokeGrant(String clientId) {
        driver.findElement(By.id("revoke-" + clientId)).click();
    }

    public Map<String, AppEntry> getApplications() {
        Map<String, AppEntry> table = new HashMap<String, AppEntry>();
        for (WebElement r : driver.findElements(By.tagName("tr"))) {
            int count = 0;
            AppEntry currentEntry = null;

            for (WebElement col : r.findElements(By.tagName("td"))) {
                count++;
                switch (count) {
                    case 1:
                        currentEntry = new AppEntry();
                        String client = col.getText();
                        table.put(client, currentEntry);
                        break;
                    case 2:
                        String rolesStr = col.getText();
                        String[] roles = rolesStr.split(",");
                        for (String role : roles) {
                            role = role.trim();
                            currentEntry.addAvailableRole(role);
                        }
                        break;
                    case 3:
                        rolesStr = col.getText();
                        if (rolesStr.isEmpty()) break;
                        roles = rolesStr.split(",");
                        for (String role : roles) {
                            role = role.trim();
                            currentEntry.addGrantedRole(role);
                        }
                        break;
                    case 4:
                        String protMappersStr = col.getText();
                        if (protMappersStr.isEmpty()) break;
                        String[] protMappers = protMappersStr.split(",");
                        for (String protMapper : protMappers) {
                            protMapper = protMapper.trim();
                            currentEntry.addMapper(protMapper);
                        }
                        break;
                    case 5:
                        String additionalGrant = col.getText();
                        if (additionalGrant.isEmpty()) break;
                        String[] grants = additionalGrant.split(",");
                        for (String grant : grants) {
                            grant = grant.trim();
                            currentEntry.addAdditionalGrant(grant);
                        }
                        break;
                }
            }
        }
        table.remove("Application");
        return table;
    }

    public static class AppEntry {

        private final List<String> rolesAvailable = new ArrayList<String>();
        private final List<String> rolesGranted = new ArrayList<String>();
        private final List<String> protocolMappersGranted = new ArrayList<String>();
        private final List<String> additionalGrants = new ArrayList<>();

        private void addAvailableRole(String role) {
            rolesAvailable.add(role);
        }

        private void addGrantedRole(String role) {
            rolesGranted.add(role);
        }

        private void addMapper(String protocolMapper) {
            protocolMappersGranted.add(protocolMapper);
        }

        private void addAdditionalGrant(String grant) {
            additionalGrants.add(grant);
        }

        public List<String> getRolesGranted() {
            return rolesGranted;
        }

        public List<String> getRolesAvailable() {
            return rolesAvailable;
        }

        public List<String> getProtocolMappersGranted() {
            return protocolMappersGranted;
        }

        public List<String> getAdditionalGrants() {
            return additionalGrants;
        }
    }
}
