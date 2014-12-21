package org.keycloak.testsuite.excluded;

import io.undertow.servlet.api.*;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.*;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testutils.KeycloakServer;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by michigerber on 21.12.14.
 */
public class ExcludedResourceTest {

    private static KeycloakServer server;

    @BeforeClass
    public static void before() throws Throwable {
        server = new KeycloakServer();
        server.start();
        importRealm();
        deployApp();
    }

    private static void importRealm() {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setId("test");
        realm.setRealm("test");
        realm.setEnabled(true);
        realm.setPrivateKey("MIICXAIBAAKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQABAoGAfmO8gVhyBxdqlxmIuglbz8bcjQbhXJLR2EoS8ngTXmN1bo2L90M0mUKSdc7qF10LgETBzqL8jYlQIbt+e6TH8fcEpKCjUlyq0Mf/vVbfZSNaVycY13nTzo27iPyWQHK5NLuJzn1xvxxrUeXI6A2WFpGEBLbHjwpx5WQG9A+2scECQQDvdn9NE75HPTVPxBqsEd2z10TKkl9CZxu10Qby3iQQmWLEJ9LNmy3acvKrE3gMiYNWb6xHPKiIqOR1as7L24aTAkEAtyvQOlCvr5kAjVqrEKXalj0Tzewjweuxc0pskvArTI2Oo070h65GpoIKLc9jf+UA69cRtquwP93aZKtW06U8dQJAF2Y44ks/mK5+eyDqik3koCI08qaC8HYq2wVl7G2QkJ6sbAaILtcvD92ToOvyGyeE0flvmDZxMYlvaZnaQ0lcSQJBAKZU6umJi3/xeEbkJqMfeLclD27XGEFoPeNrmdx0q10Azp4NfJAY+Z8KRyQCR2BEG+oNitBOZ+YXF9KCpH3cdmECQHEigJhYg+ykOvr1aiZUMFT72HU0jnmQe2FVekuG+LJUt2Tm7GtMjTFoGpf0JwrVuZN39fOYAlo+nTixgeW7X8Y=");
        realm.setPublicKey("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB");
        realm.setPasswordCredentialGrantAllowed(true);
        realm.setRequiredCredentials(new HashSet<String>(Arrays.asList(CredentialRepresentation.PASSWORD)));

        ApplicationRepresentation applicationRepresentation = new ApplicationRepresentation();
        applicationRepresentation.setName("app");
        applicationRepresentation.setBaseUrl("/app");
        applicationRepresentation.setPublicClient(true);
        applicationRepresentation.setEnabled(true);
        applicationRepresentation.setRedirectUris(Arrays.asList("/app/*"));
        realm.setApplications(Arrays.asList(applicationRepresentation));

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setId("user");
        userRepresentation.setUsername("user");
        userRepresentation.setEnabled(true);
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue("password");
        userRepresentation.setCredentials(Arrays.asList(credential));
        userRepresentation.setRealmRoles(Arrays.asList("user"));
        realm.setUsers(Arrays.asList(userRepresentation));

        RoleRepresentation roleRepresentation = new RoleRepresentation();
        roleRepresentation.setName("user");
        RolesRepresentation rolesRepresentation = new RolesRepresentation();
        rolesRepresentation.setRealm(Arrays.asList(roleRepresentation));
        realm.setRoles(rolesRepresentation);

        server.importRealm(realm);
    }

    private static void deployApp() {

        ResteasyDeployment resteasyDeployment = new ResteasyDeployment();
        DeploymentInfo deploymentInfo = new DeploymentInfo();
        deploymentInfo.setDeploymentName("app");
        deploymentInfo.setContextPath("/app");
        deploymentInfo.setClassLoader(ExcludedResourceTest.class.getClassLoader());

        ServletInfo servletInfo = new ServletInfo("post", PostServlet.class);
        servletInfo.addMappings("/public");
        servletInfo.addMappings("/secure");
        deploymentInfo.addServlet(servletInfo);


        SecurityConstraint publicConstraint = new SecurityConstraint();
        WebResourceCollection publicResource = new WebResourceCollection();
        publicResource.addUrlPattern("/public/*");
        publicConstraint.addWebResourceCollection(publicResource);
        publicConstraint.setEmptyRoleSemantic(SecurityInfo.EmptyRoleSemantic.PERMIT);
        deploymentInfo.addSecurityConstraint(publicConstraint);

        SecurityConstraint secureConstraint = new SecurityConstraint();
        WebResourceCollection secureResources = new WebResourceCollection();
        secureResources.addUrlPattern("/*");
        secureConstraint.addWebResourceCollection(secureResources);
        secureConstraint.addRoleAllowed("user");
        deploymentInfo.addSecurityConstraint(secureConstraint);
        deploymentInfo.addSecurityRole("user");

        deploymentInfo.setLoginConfig(new LoginConfig("KEYCLOAK", "test"));

        deploymentInfo.addInitParameter("keycloak.config.file", ExcludedResourceTest.class.getResource("keycloak.json").getFile());

        server.getServer().deploy(deploymentInfo);
    }

    @AfterClass
    public static void after() {
        server.stop();
    }

    @Test
    public void testPublicWithoutToken() throws InterruptedException {
        Response post = ClientBuilder.newClient()
                .target("http://localhost:8081/app")
                .path("public")
                .request()
                .post(Entity.entity("Hallo", MediaType.TEXT_PLAIN_TYPE));

        String response = post.readEntity(String.class);
        Assert.assertEquals("you said: Hallo", response);
    }

    @Test
    public void testSecureWithoutToken() throws InterruptedException {
        Response post = ClientBuilder.newClient()
                .target("http://localhost:8081/app")
                .path("secure")
                .request()
                .post(Entity.entity("Hallo", MediaType.TEXT_PLAIN_TYPE));

        //Redirect to login
        Assert.assertEquals(302, post.getStatusInfo().getStatusCode());
    }
    @Test
    public void testSecureWithToken() throws InterruptedException, JSONException {
        Form form = new Form();
        form.param("username","user");
        form.param("password","password");
        form.param("client_id","app");

        AccessTokenResponse token = ClientBuilder.newClient()
                .target("http://localhost:8081/auth")
                .path("/realms/test/tokens/grants/access")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.form(form))
                .readEntity(AccessTokenResponse.class);

        Response post = ClientBuilder.newClient()
                .target("http://localhost:8081/app")
                .path("secure")
                .request()
                .header("Authorization", "Bearer " + token.getToken())
                .post(Entity.entity("Hallo", MediaType.TEXT_PLAIN_TYPE));

        String response = post.readEntity(String.class);
        Assert.assertEquals("Hello user, you said: Hallo", response);

    }

    @Test
    public void testPublicWithToken() throws InterruptedException, JSONException {
        Form form = new Form();
        form.param("username","user");
        form.param("password","password");
        form.param("client_id","app");

        AccessTokenResponse token = ClientBuilder.newClient()
                .target("http://localhost:8081/auth")
                .path("/realms/test/tokens/grants/access")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.form(form))
                .readEntity(AccessTokenResponse.class);

        Response post = ClientBuilder.newClient()
                .target("http://localhost:8081/app")
                .path("public")
                .request()
                .header("Authorization", "Bearer " + token.getToken())
                .post(Entity.entity("Hallo", MediaType.TEXT_PLAIN_TYPE));

        String response = post.readEntity(String.class);
        Assert.assertEquals("Hello user, you said: Hallo", response);
    }

}
