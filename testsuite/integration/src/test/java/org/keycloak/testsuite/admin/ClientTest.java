package org.keycloak.testsuite.admin;

import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

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
public class ClientTest extends AbstractClientTest {

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @Test
    public void getClients() {
        assertNames(realm.clients().findAll(), "account", "realm-management", "security-admin-console");
    }

    @Test
    public void createClient() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId("my-app");
        rep.setEnabled(true);
        realm.clients().create(rep);

        assertNames(realm.clients().findAll(), "account", "realm-management", "security-admin-console", "my-app");
    }

    @Test
    public void removeClient() {
        createClient();

        realm.clients().get("my-app").remove();
    }

    @Test
    public void getClientRepresentation() {
        createClient();

        ClientRepresentation rep = realm.clients().get("my-app").toRepresentation();
        assertEquals("my-app", rep.getClientId());
        assertTrue(rep.isEnabled());
    }

    @Test
    public void getClientSessions() throws Exception {
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "password");
        assertEquals(200, response.getStatusCode());

        OAuthClient.AuthorizationCodeResponse codeResponse = oauth.doLogin("test-user@localhost", "password");

        OAuthClient.AccessTokenResponse response2 = oauth.doAccessTokenRequest(codeResponse.getCode(), "password");
        assertEquals(200, response2.getStatusCode());

        ClientResource app = keycloak.realm("test").clients().get("test-app");

        assertEquals(2, (long) app.getApplicationSessionCount().get("count"));

        List<UserSessionRepresentation> userSessions = app.getUserSessions(0, 100);
        assertEquals(2, userSessions.size());
        assertEquals(1, userSessions.get(0).getClients().size());
    }

    @Test
    // KEYCLOAK-1110
    public void deleteDefaultRole() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId("my-app");
        rep.setEnabled(true);
        realm.clients().create(rep);

        RoleRepresentation role = new RoleRepresentation("test", "test");
        realm.clients().get("my-app").roles().create(role);

        rep = realm.clients().get("my-app").toRepresentation();
        rep.setDefaultRoles(new String[] { "test" });
        realm.clients().get("my-app").update(rep);

        assertArrayEquals(new String[] { "test" }, realm.clients().get("my-app").toRepresentation().getDefaultRoles());

        realm.clients().get("my-app").roles().deleteRole("test");

        assertNull(realm.clients().get("my-app").toRepresentation().getDefaultRoles());
    }

    @Test
    public void testProtocolMappers() {
        createClient();
        ProtocolMappersResource mappersResource = realm.clients().get("my-app").getProtocolMappers();

        protocolMappersTest(mappersResource);
    }


    public static void protocolMappersTest(ProtocolMappersResource mappersResource) {
        // assert default mappers found
        List<ProtocolMapperRepresentation> protocolMappers = mappersResource.getMappers();

        String emailMapperId = null;
        String usernameMapperId = null;
        String fooMapperId = null;
        for (ProtocolMapperRepresentation mapper : protocolMappers) {
            if (mapper.getName().equals(OIDCLoginProtocolFactory.EMAIL)) {
                emailMapperId = mapper.getId();
            } else if (mapper.getName().equals(OIDCLoginProtocolFactory.USERNAME)) {
                usernameMapperId = mapper.getId();
            } else if (mapper.getName().equals("foo")) {
                fooMapperId = mapper.getId();
            }
        }

        assertNotNull(emailMapperId);
        assertNotNull(usernameMapperId);
        assertNull(fooMapperId);

        // Create foo mapper
        ProtocolMapperRepresentation fooMapper = new ProtocolMapperRepresentation();
        fooMapper.setName("foo");
        fooMapper.setProtocol("fooProtocol");
        fooMapper.setProtocolMapper("fooMapper");
        fooMapper.setConsentRequired(true);
        Response response = mappersResource.createMapper(fooMapper);
        String location = response.getLocation().toString();
        fooMapperId = location.substring(location.lastIndexOf("/") + 1);
        response.close();

        fooMapper = mappersResource.getMapperById(fooMapperId);
        assertEquals(fooMapper.getName(), "foo");

        // Update foo mapper
        fooMapper.setProtocolMapper("foo-mapper-updated");
        mappersResource.update(fooMapperId, fooMapper);

        fooMapper = mappersResource.getMapperById(fooMapperId);
        assertEquals(fooMapper.getProtocolMapper(), "foo-mapper-updated");

        // Remove foo mapper
        mappersResource.delete(fooMapperId);
        try {
            mappersResource.getMapperById(fooMapperId);
            fail("Not expected to find deleted mapper");
        } catch (NotFoundException nfe) {
        }
    }
}
