package org.keycloak.testsuite.saml;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.Config;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlBindingTest {

    @ClassRule
    public static SamlKeycloakRule keycloakRule = new SamlKeycloakRule() {
        @Override
        public void initWars() {
             ClassLoader classLoader = SamlBindingTest.class.getClassLoader();

            initializeSamlSecuredWar("/saml/simple-post", "/sales-post",  "post.war", classLoader);
            initializeSamlSecuredWar("/saml/signed-post", "/sales-post-sig",  "post-sig.war", classLoader);
            initializeSamlSecuredWar("/saml/signed-post-email", "/sales-post-sig-email",  "post-sig-email.war", classLoader);
            initializeSamlSecuredWar("/saml/signed-post-transient", "/sales-post-sig-transient",  "post-sig-transient.war", classLoader);
            initializeSamlSecuredWar("/saml/signed-post-persistent", "/sales-post-sig-persistent",  "post-sig-persistent.war", classLoader);
            initializeSamlSecuredWar("/saml/signed-metadata", "/sales-metadata",  "post-metadata.war", classLoader);
            initializeSamlSecuredWar("/saml/signed-get", "/employee-sig",  "employee-sig.war", classLoader);
            initializeSamlSecuredWar("/saml/signed-front-get", "/employee-sig-front",  "employee-sig-front.war", classLoader);
            initializeSamlSecuredWar("/saml/bad-client-signed-post", "/bad-client-sales-post-sig",  "bad-client-post-sig.war", classLoader);
            initializeSamlSecuredWar("/saml/bad-realm-signed-post", "/bad-realm-sales-post-sig",  "bad-realm-post-sig.war", classLoader);
            initializeSamlSecuredWar("/saml/encrypted-post", "/sales-post-enc",  "post-enc.war", classLoader);
            uploadSP();

        }

        @Override
        public String getRealmJson() {
            return "/saml/testsaml.json";
        }
    };

    @Rule
    public WebRule webRule = new WebRule(this);
    @WebResource
    protected WebDriver driver;
    @WebResource
    protected LoginPage loginPage;

    //@Test
    public void runit() throws Exception {
        Thread.sleep(10000000);
    }

    protected void checkLoggedOut(String mainUrl) {
        String pageSource = driver.getPageSource();
        System.out.println("*** logout pagesouce ***");
        System.out.println(pageSource);
        System.out.println("driver url: " + driver.getCurrentUrl());
        Assert.assertTrue(pageSource.contains("request-path: /logout.jsp"));
        driver.navigate().to(mainUrl);
        Assert.assertTrue(driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/demo/protocol/saml"));
    }


    @Test
    public void testPostSimpleLoginLogout() {
        driver.navigate().to("http://localhost:8081/sales-post/");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/auth/realms/demo/protocol/saml");
        loginPage.login("bburke", "password");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/sales-post/");
        System.out.println(driver.getPageSource());
        Assert.assertTrue(driver.getPageSource().contains("bburke"));
        driver.navigate().to("http://localhost:8081/sales-post?GLO=true");
        checkLoggedOut("http://localhost:8081/sales-post/");
    }
    @Test
    public void testPostSignedLoginLogout() {
        driver.navigate().to("http://localhost:8081/sales-post-sig/");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/auth/realms/demo/protocol/saml");
        loginPage.login("bburke", "password");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/sales-post-sig/");
        Assert.assertTrue(driver.getPageSource().contains("bburke"));
        driver.navigate().to("http://localhost:8081/sales-post-sig?GLO=true");
        checkLoggedOut("http://localhost:8081/sales-post-sig/");

    }
    @Test
    public void testPostSignedLoginLogoutTransientNameID() {
        driver.navigate().to("http://localhost:8081/sales-post-sig-transient/");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/auth/realms/demo/protocol/saml");
        loginPage.login("bburke", "password");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/sales-post-sig-transient/");
        System.out.println(driver.getPageSource());
        Assert.assertFalse(driver.getPageSource().contains("bburke"));
        Assert.assertTrue(driver.getPageSource().contains("principal=G-"));
        driver.navigate().to("http://localhost:8081/sales-post-sig-transient?GLO=true");
        checkLoggedOut("http://localhost:8081/sales-post-sig-transient/");

    }
    @Test
    public void testPostSignedLoginLogoutPersistentNameID() {
        driver.navigate().to("http://localhost:8081/sales-post-sig-persistent/");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/auth/realms/demo/protocol/saml");
        loginPage.login("bburke", "password");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/sales-post-sig-persistent/");
        System.out.println(driver.getPageSource());
        Assert.assertFalse(driver.getPageSource().contains("bburke"));
        Assert.assertTrue(driver.getPageSource().contains("principal=G-"));
        driver.navigate().to("http://localhost:8081/sales-post-sig-persistent?GLO=true");
        checkLoggedOut("http://localhost:8081/sales-post-sig-persistent/");

    }
    @Test
    public void testPostSignedLoginLogoutEmailNameID() {
        driver.navigate().to("http://localhost:8081/sales-post-sig-email/");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/auth/realms/demo/protocol/saml");
        loginPage.login("bburke", "password");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/sales-post-sig-email/");
        System.out.println(driver.getPageSource());
        Assert.assertTrue(driver.getPageSource().contains("principal=bburke@redhat.com"));
        driver.navigate().to("http://localhost:8081/sales-post-sig-email?GLO=true");
        checkLoggedOut("http://localhost:8081/sales-post-sig-email/");

    }
    @Test
    public void testRedirectSignedLoginLogout() {
        driver.navigate().to("http://localhost:8081/employee-sig/");
        Assert.assertTrue(driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/demo/protocol/saml"));
        loginPage.login("bburke", "password");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/employee-sig/");
        Assert.assertTrue(driver.getPageSource().contains("bburke"));
        driver.navigate().to("http://localhost:8081/employee-sig?GLO=true");
        checkLoggedOut("http://localhost:8081/employee-sig/");

    }

    @Test
    public void testRedirectSignedLoginLogoutFrontNoSSO() {
        driver.navigate().to("http://localhost:8081/employee-sig-front/");
        Assert.assertTrue(driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/demo/protocol/saml"));
        loginPage.login("bburke", "password");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/employee-sig-front/");
        Assert.assertTrue(driver.getPageSource().contains("bburke"));
        driver.navigate().to("http://localhost:8081/employee-sig-front?GLO=true");
        checkLoggedOut("http://localhost:8081/employee-sig-front/");

    }

    @Test
    public void testRedirectSignedLoginLogoutFront() {
        // visit 1st app an logg in
        System.out.println("visit 1st app ");
        driver.navigate().to("http://localhost:8081/employee-sig/");
        Assert.assertTrue(driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/demo/protocol/saml"));
        System.out.println("login to form");
        loginPage.login("bburke", "password");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/employee-sig/");
        Assert.assertTrue(driver.getPageSource().contains("bburke"));

        // visit 2nd app
        System.out.println("visit 2nd app ");
        driver.navigate().to("http://localhost:8081/employee-sig-front/");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/employee-sig-front/");
        Assert.assertTrue(driver.getPageSource().contains("bburke"));

        // logout of first app
        System.out.println("GLO");
        driver.navigate().to("http://localhost:8081/employee-sig?GLO=true");
        checkLoggedOut("http://localhost:8081/employee-sig/");
        driver.navigate().to("http://localhost:8081/employee-sig-front/");
        Assert.assertTrue(driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/demo/protocol/saml"));

    }

    @Test
    public void testPostEncryptedLoginLogout() {
        driver.navigate().to("http://localhost:8081/sales-post-enc/");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/auth/realms/demo/protocol/saml");
        loginPage.login("bburke", "password");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/sales-post-enc/");
        Assert.assertTrue(driver.getPageSource().contains("bburke"));
        driver.navigate().to("http://localhost:8081/sales-post-enc?GLO=true");
        checkLoggedOut("http://localhost:8081/sales-post-enc/");

    }
    @Test
    public void testPostBadClientSignature() {
        driver.navigate().to("http://localhost:8081/bad-client-sales-post-sig/");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/auth/realms/demo/protocol/saml");
        Assert.assertEquals(driver.getTitle(), "We're sorry...");

    }

    @Test
    public void testPostBadRealmSignature() {
        driver.navigate().to("http://localhost:8081/bad-realm-sales-post-sig/");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/auth/realms/demo/protocol/saml");
        loginPage.login("bburke", "password");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/bad-realm-sales-post-sig/");
        Assert.assertTrue(driver.getPageSource().contains("null"));
    }

    private static String createToken() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);

            RealmModel adminRealm = manager.getRealm(Config.getAdminRealm());
            ApplicationModel adminConsole = adminRealm.getApplicationByName(Constants.ADMIN_CONSOLE_APPLICATION);
            TokenManager tm = new TokenManager();
            UserModel admin = session.users().getUserByUsername("admin", adminRealm);
            UserSessionModel userSession = session.sessions().createUserSession(adminRealm, admin, "admin", null, "form", false);
            AccessToken token = tm.createClientAccessToken(tm.getAccess(null, adminConsole, admin), adminRealm, adminConsole, admin, userSession);
            return tm.encodeToken(adminRealm, token);
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }


    @Test
    public void testMetadataPostSignedLoginLogout() throws Exception {

        driver.navigate().to("http://localhost:8081/sales-metadata/");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/auth/realms/demo/protocol/saml");
        loginPage.login("bburke", "password");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/sales-metadata/");
        String pageSource = driver.getPageSource();
        Assert.assertTrue(pageSource.contains("bburke"));
        driver.navigate().to("http://localhost:8081/sales-metadata?GLO=true");
        checkLoggedOut("http://localhost:8081/sales-metadata/");

    }

    public static void uploadSP() {
        String token = createToken();
        final String authHeader = "Bearer " + token;
        ClientRequestFilter authFilter = new ClientRequestFilter() {
            @Override
            public void filter(ClientRequestContext requestContext) throws IOException {
                requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, authHeader);
            }
        };
        Client client = ClientBuilder.newBuilder().register(authFilter).build();
        UriBuilder authBase = UriBuilder.fromUri("http://localhost:8081/auth");
        WebTarget adminRealms = client.target(AdminRoot.realmsUrl(authBase));


        MultipartFormDataOutput formData = new MultipartFormDataOutput();
        InputStream is = SamlBindingTest.class.getResourceAsStream("/saml/sp-metadata.xml");
        Assert.assertNotNull(is);
        formData.addFormData("file", is, MediaType.APPLICATION_XML_TYPE);

        WebTarget upload = adminRealms.path("demo/application-importers/saml2-entity-descriptor/upload");
        System.out.println(upload.getUri());
        Response response = upload.request().post(Entity.entity(formData, MediaType.MULTIPART_FORM_DATA));
        Assert.assertEquals(204, response.getStatus());
        response.close();
        client.close();
    }


}
