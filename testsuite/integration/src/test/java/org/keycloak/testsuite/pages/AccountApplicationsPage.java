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

    public Map<String, ClientGrant> getClientGrants() {
        Map<String, ClientGrant> table = new HashMap<String, ClientGrant>();
        for (WebElement r : driver.findElements(By.tagName("tr"))) {
            int count = 0;
            ClientGrant currentGrant = null;

            for (WebElement col : r.findElements(By.tagName("td"))) {
                count++;
                switch (count) {
                    case 1:
                        currentGrant = new ClientGrant();
                        String clientId = col.getText();
                        table.put(clientId, currentGrant);
                        break;
                    case 2:
                        String protMappersStr = col.getText();
                        String[] protMappers = protMappersStr.split(",");
                        for (String protMapper : protMappers) {
                            protMapper = protMapper.trim();
                            currentGrant.addMapper(protMapper);
                        }
                        break;
                    case 3:
                        String rolesStr = col.getText();
                        String[] roles = rolesStr.split(",");
                        for (String role : roles) {
                            role = role.trim();
                            currentGrant.addRole(role);
                        }
                        break;
                }
            }
        }
        table.remove("Client");
        return table;
    }

    public static class ClientGrant {

        private final List<String> protocolMapperDescriptions = new ArrayList<String>();
        private final List<String> roleDescriptions = new ArrayList<String>();

        private void addMapper(String protocolMapper) {
            protocolMapperDescriptions.add(protocolMapper);
        }

        private void addRole(String role) {
            roleDescriptions.add(role);
        }

        public List<String> getProtocolMapperDescriptions() {
            return protocolMapperDescriptions;
        }

        public List<String> getRoleDescriptions() {
            return roleDescriptions;
        }
    }
}
