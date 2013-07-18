package org.keycloak.test;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.test.EmbeddedContainer;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.SkeletonKeyContextResolver;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredCredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.InstallationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.picketlink.idm.IdentitySession;
import org.picketlink.idm.model.Realm;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.client.Client;
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
public class RealmCreationTest {

    private static ResteasyDeployment deployment;
    private static Client client;

    @BeforeClass
    public static void before() throws Exception {
        deployment = new ResteasyDeployment();
        deployment.setApplicationClass(KeycloakApplication.class.getName());
        EmbeddedContainer.start(deployment);
        KeycloakApplication application = (KeycloakApplication) deployment.getApplication();
        IdentitySession IdentitySession = application.getFactory().createIdentitySession();
        RealmManager manager = new RealmManager(IdentitySession);
        new InstallationManager().install(manager);
        client = new ResteasyClientBuilder().build();
        client.register(SkeletonKeyContextResolver.class);
    }

    public static void after() throws Exception {
        client.close();
        EmbeddedContainer.stop();
    }

    @Test
    public void testRegisterLoginAndCreate() throws Exception {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("bburke");
        user.credential(RequiredCredentialRepresentation.PASSWORD, "geheim", false);

        WebTarget target = client.target(generateURL("/"));
        Response response = target.path("registrations").request().post(Entity.json(user));
        Assert.assertEquals(201, response.getStatus());
        response.close();


        AccessTokenResponse tokenResponse = null;
        try {
            Form form = new Form();
            form.param(AuthenticationManager.FORM_USERNAME, "bburke");
            form.param(RequiredCredentialRepresentation.PASSWORD, "badpassword");
            tokenResponse = target.path("realms").path(Realm.DEFAULT_REALM).path("tokens/grants/identity-token").request().post(Entity.form(form), AccessTokenResponse.class);
            Assert.fail();
        } catch (NotAuthorizedException e) {
        }
        Form form = new Form();
        form.param(AuthenticationManager.FORM_USERNAME, "bburke");
        form.param(RequiredCredentialRepresentation.PASSWORD, "geheim");
        tokenResponse = target.path("realms").path(Realm.DEFAULT_REALM).path("tokens/grants/identity-token").request().post(Entity.form(form), AccessTokenResponse.class);
        Assert.assertNotNull(tokenResponse);
        System.out.println(tokenResponse.getToken());
        //

        RealmRepresentation realm = KeycloakTestBase.loadJson("testrealm.json");
        response = target.path("realms").request().header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenResponse.getToken()).post(Entity.json(realm));
        Assert.assertEquals(201, response.getStatus());
        response.close();
    }
}
