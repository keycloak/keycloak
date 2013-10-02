package org.keycloak.test;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import static org.jboss.resteasy.test.TestPortProvider.generateURL;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmCreationTest extends AbstractKeycloakServerTest {

    @BeforeClass
    public static void before() throws Exception {
        KeycloakSession session = application.getFactory().createSession();
        session.getTransaction().begin();
        RealmManager manager = new RealmManager(session);
        new InstallationManager().install(manager);
        session.getTransaction().commit();
        session.close();
    }

    @Test
    public void testRegisterLoginAndCreate() throws Exception {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("bburke");
        user.credential(CredentialRepresentation.PASSWORD, "geheim");

        WebTarget target = client.target(generateURL("/"));
        Response response = target.path("saas/registrations").request().post(Entity.json(user));
        Assert.assertEquals(201, response.getStatus());
        response.close();


        AccessTokenResponse tokenResponse = null;
        try {
            Form form = new Form();
            form.param(AuthenticationManager.FORM_USERNAME, "bburke");
            form.param(CredentialRepresentation.PASSWORD, "badpassword");
            tokenResponse = target.path("realms").path(RealmModel.DEFAULT_REALM).path("tokens/grants/identity-token").request().post(Entity.form(form), AccessTokenResponse.class);
            Assert.fail();
        } catch (NotAuthorizedException e) {
        }
        Form form = new Form();
        form.param(AuthenticationManager.FORM_USERNAME, "bburke");
        form.param(CredentialRepresentation.PASSWORD, "geheim");
        tokenResponse = target.path("realms").path(RealmModel.DEFAULT_REALM).path("tokens/grants/identity-token").request().post(Entity.form(form), AccessTokenResponse.class);
        Assert.assertNotNull(tokenResponse);
        System.out.println(tokenResponse.getToken());
        //

        RealmRepresentation realm = loadJson("testrealm.json");
        response = target.path("saas/admin/realms").request().header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenResponse.getToken()).post(Entity.json(realm));
        Assert.assertEquals(201, response.getStatus());
        response.close();
    }
}
