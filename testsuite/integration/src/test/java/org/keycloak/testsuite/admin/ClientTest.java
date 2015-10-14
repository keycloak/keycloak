package org.keycloak.testsuite.admin;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
 * @author <a href="mailto:tom@tutorials.de">Thomas Darimont</a>
 */
public class ClientTest extends AbstractClientTest {

    @Rule
    public WebRule webRule = new WebRule(this);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @Test
    public void getClients() {
        assertNames(realm.clients().findAll(), "account", "realm-management", "security-admin-console", "broker");
    }

    private String createClient() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId("my-app");
        rep.setEnabled(true);
        Response response = realm.clients().create(rep);
        response.close();
        return ApiUtil.getCreatedId(response);
    }

    @Test
    public void createClientVerify() {
        String id = createClient();

        assertNotNull(realm.clients().get(id));
        assertNames(realm.clients().findAll(), "account", "realm-management", "security-admin-console", "broker", "my-app");
    }

    @Test
    public void removeClient() {
        String id = createClient();

        realm.clients().get(id).remove();
    }

    @Test
    public void getClientRepresentation() {
        String id = createClient();

        ClientRepresentation rep = realm.clients().get(id).toRepresentation();
        assertEquals(id, rep.getId());
        assertEquals("my-app", rep.getClientId());
        assertTrue(rep.isEnabled());
    }

    /**
     * See <a href="https://issues.jboss.org/browse/KEYCLOAK-1963">KEYCLOAK-1963</a>
     */
    @Test
    public void getClientByClientId_withKnownClient() {

        String id = createClient();

        ClientRepresentation rep = realm.clients().getByClientId("my-app").toRepresentation();

        assertEquals(id, rep.getId());
        assertEquals("my-app", rep.getClientId());
        assertTrue(rep.isEnabled());
    }

    /**
     * See <a href="https://issues.jboss.org/browse/KEYCLOAK-1963">KEYCLOAK-1963</a>
     */
    @Test
    public void getClientByClientId_withUnknownClient() {

        expectedException.expect(NotFoundException.class);

        realm.clients().getByClientId("does-not-exist").toRepresentation();
    }

    @Test
    public void getClientSessions() throws Exception {
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "password");
        assertEquals(200, response.getStatusCode());

        OAuthClient.AuthorizationCodeResponse codeResponse = oauth.doLogin("test-user@localhost", "password");

        OAuthClient.AccessTokenResponse response2 = oauth.doAccessTokenRequest(codeResponse.getCode(), "password");
        assertEquals(200, response2.getStatusCode());

        ClientResource app = ApiUtil.findClientByClientId(keycloak.realm("test"), "test-app");

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
        Response response = realm.clients().create(rep);
        response.close();
        String id = ApiUtil.getCreatedId(response);

        RoleRepresentation role = new RoleRepresentation("test", "test", false);
        realm.clients().get(id).roles().create(role);

        rep = realm.clients().get(id).toRepresentation();
        rep.setDefaultRoles(new String[] { "test" });
        realm.clients().get(id).update(rep);

        assertArrayEquals(new String[] { "test" }, realm.clients().get(id).toRepresentation().getDefaultRoles());

        realm.clients().get(id).roles().deleteRole("test");

        assertNull(realm.clients().get(id).toRepresentation().getDefaultRoles());
    }

    @Test
    public void testProtocolMappers() {
        createClient();
        ProtocolMappersResource mappersResource = ApiUtil.findClientByClientId(realm, "my-app").getProtocolMappers();

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
