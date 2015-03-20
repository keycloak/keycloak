package org.keycloak.testsuite.admin;

import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.ApplicationResource;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.NotFoundException;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ApplicationTest extends AbstractClientTest {

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @Test
    public void getApplications() {
        assertNames(realm.applications().findAll(), "account", "realm-management", "security-admin-console");
    }

    @Test
    public void createApplication() {
        ApplicationRepresentation rep = new ApplicationRepresentation();
        rep.setName("my-app");
        rep.setEnabled(true);
        realm.applications().create(rep);

        assertNames(realm.applications().findAll(), "account", "realm-management", "security-admin-console", "my-app");
    }

    @Test
    public void removeApplication() {
        createApplication();

        realm.applications().get("my-app").remove();
    }

    @Test
    public void getApplicationRepresentation() {
        createApplication();

        ApplicationRepresentation rep = realm.applications().get("my-app").toRepresentation();
        assertEquals("my-app", rep.getName());
        assertTrue(rep.isEnabled());
    }

    @Test
    public void getApplicationSessions() throws Exception {
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "password");
        assertEquals(200, response.getStatusCode());

        OAuthClient.AuthorizationCodeResponse codeResponse = oauth.doLogin("test-user@localhost", "password");

        OAuthClient.AccessTokenResponse response2 = oauth.doAccessTokenRequest(codeResponse.getCode(), "password");
        assertEquals(200, response2.getStatusCode());

        ApplicationResource app = keycloak.realm("test").applications().get("test-app");

        assertEquals(2, (long) app.getApplicationSessionCount().get("count"));

        List<UserSessionRepresentation> userSessions = app.getUserSessions(0, 100);
        assertEquals(2, userSessions.size());
        assertEquals(1, userSessions.get(0).getApplications().size());
    }

    @Test
    // KEYCLOAK-1110
    public void deleteDefaultRole() {
        ApplicationRepresentation rep = new ApplicationRepresentation();
        rep.setName("my-app");
        rep.setEnabled(true);
        realm.applications().create(rep);

        RoleRepresentation role = new RoleRepresentation("test", "test");
        realm.applications().get("my-app").roles().create(role);

        rep = realm.applications().get("my-app").toRepresentation();
        rep.setDefaultRoles(new String[] { "test" });
        realm.applications().get("my-app").update(rep);

        assertArrayEquals(new String[] { "test" }, realm.applications().get("my-app").toRepresentation().getDefaultRoles());

        realm.applications().get("my-app").roles().deleteRole("test");

        assertNull(realm.applications().get("my-app").toRepresentation().getDefaultRoles());
    }

}
