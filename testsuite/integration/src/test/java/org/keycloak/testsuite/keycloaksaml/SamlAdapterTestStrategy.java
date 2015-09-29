package org.keycloak.testsuite.keycloaksaml;

import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.keycloak.Config;
import org.keycloak.adapters.saml.SamlPrincipal;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.HardcodedAttributeMapper;
import org.keycloak.protocol.saml.mappers.HardcodedRole;
import org.keycloak.protocol.saml.mappers.RoleListMapper;
import org.keycloak.protocol.saml.mappers.RoleNameMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.saml.processing.core.saml.v2.constants.X500SAMLProfileConstants;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.testsuite.KeycloakServer;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.keycloak.util.JsonSerialization;
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
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlAdapterTestStrategy  extends ExternalResource {
    protected String AUTH_SERVER_URL = "http://localhost:8081/auth";
    protected String APP_SERVER_BASE_URL = "http://localhost:8081";
    protected AbstractKeycloakRule keycloakRule;

    public SamlAdapterTestStrategy(String AUTH_SERVER_URL, String APP_SERVER_BASE_URL, AbstractKeycloakRule keycloakRule) {
        this.AUTH_SERVER_URL = AUTH_SERVER_URL;
        this.APP_SERVER_BASE_URL = APP_SERVER_BASE_URL;
        this.keycloakRule = keycloakRule;
    }

    public WebRule webRule = new WebRule(this);


    @WebResource
    protected WebDriver driver;
    @WebResource
    protected LoginPage loginPage;

    @Override
    protected void before() throws Throwable {
        super.before();
        webRule.before();
    }

    @Override
    protected void after() {
        super.after();
        webRule.after();
    }

    public static RealmModel baseAdapterTestInitialization(KeycloakSession session, RealmManager manager, RealmModel adminRealm, Class<?> clazz) {
        RealmRepresentation representation = KeycloakServer.loadJson(clazz.getResourceAsStream("/keycloak-saml/testsaml.json"), RealmRepresentation.class);
        RealmModel demoRealm = manager.importRealm(representation);
        return demoRealm;
    }



    protected void checkLoggedOut(String mainUrl) {
        String pageSource = driver.getPageSource();
        System.out.println("*** logout pagesource ***");
        System.out.println(pageSource);
        System.out.println("driver url: " + driver.getCurrentUrl());
        Assert.assertTrue(pageSource.contains("request-path: /logout.jsp"));
        driver.navigate().to(mainUrl);
        Assert.assertTrue(driver.getCurrentUrl().startsWith(AUTH_SERVER_URL + "/realms/demo/protocol/saml"));
    }

    public void testPostSimpleLoginLogout() {
        driver.navigate().to(APP_SERVER_BASE_URL + "/sales-post/");
        assertEquals(driver.getCurrentUrl(), AUTH_SERVER_URL + "/realms/demo/protocol/saml");
        loginPage.login("bburke", "password");
        assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/sales-post/");
        System.out.println(driver.getPageSource());
        Assert.assertTrue(driver.getPageSource().contains("bburke"));
        driver.navigate().to(APP_SERVER_BASE_URL + "/sales-post?GLO=true");
        checkLoggedOut(APP_SERVER_BASE_URL + "/sales-post/");
    }

    public void testPostSimpleUnauthorized(CheckAuthError error) {
        driver.navigate().to(APP_SERVER_BASE_URL + "/sales-post/");
        assertEquals(driver.getCurrentUrl(), AUTH_SERVER_URL + "/realms/demo/protocol/saml");
        loginPage.login("unauthorized", "password");
        assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/sales-post/");
        System.out.println(driver.getPageSource());
        error.check(driver);
    }

    public void testPostSimpleLoginLogoutIdpInitiated() {
        driver.navigate().to(AUTH_SERVER_URL + "/realms/demo/protocol/saml/clients/sales-post");
        loginPage.login("bburke", "password");
        assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/sales-post/");
        System.out.println(driver.getPageSource());
        Assert.assertTrue(driver.getPageSource().contains("bburke"));
        driver.navigate().to(APP_SERVER_BASE_URL + "/sales-post?GLO=true");
        checkLoggedOut(APP_SERVER_BASE_URL + "/sales-post/");
    }

    public void testPostSignedLoginLogout() {
        driver.navigate().to(APP_SERVER_BASE_URL + "/sales-post-sig/");
        assertEquals(driver.getCurrentUrl(), AUTH_SERVER_URL + "/realms/demo/protocol/saml");
        loginPage.login("bburke", "password");
        assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/sales-post-sig/");
        Assert.assertTrue(driver.getPageSource().contains("bburke"));
        driver.navigate().to(APP_SERVER_BASE_URL + "/sales-post-sig?GLO=true");
        checkLoggedOut(APP_SERVER_BASE_URL + "/sales-post-sig/");

    }
    public void testPostSignedLoginLogoutTransientNameID() {
        driver.navigate().to(APP_SERVER_BASE_URL + "/sales-post-sig-transient/");
        assertEquals(driver.getCurrentUrl(), AUTH_SERVER_URL + "/realms/demo/protocol/saml");
        loginPage.login("bburke", "password");
        assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/sales-post-sig-transient/");
        System.out.println(driver.getPageSource());
        Assert.assertFalse(driver.getPageSource().contains("bburke"));
        Assert.assertTrue(driver.getPageSource().contains("principal=G-"));
        driver.navigate().to(APP_SERVER_BASE_URL + "/sales-post-sig-transient?GLO=true");
        checkLoggedOut(APP_SERVER_BASE_URL + "/sales-post-sig-transient/");

    }
    public void testPostSignedLoginLogoutPersistentNameID() {
        driver.navigate().to(APP_SERVER_BASE_URL + "/sales-post-sig-persistent/");
        assertEquals(driver.getCurrentUrl(), AUTH_SERVER_URL + "/realms/demo/protocol/saml");
        loginPage.login("bburke", "password");
        assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/sales-post-sig-persistent/");
        System.out.println(driver.getPageSource());
        Assert.assertFalse(driver.getPageSource().contains("bburke"));
        Assert.assertTrue(driver.getPageSource().contains("principal=G-"));
        driver.navigate().to(APP_SERVER_BASE_URL + "/sales-post-sig-persistent?GLO=true");
        checkLoggedOut(APP_SERVER_BASE_URL + "/sales-post-sig-persistent/");

    }
    public void testPostSignedLoginLogoutEmailNameID() {
        driver.navigate().to(APP_SERVER_BASE_URL + "/sales-post-sig-email/");
        assertEquals(driver.getCurrentUrl(), AUTH_SERVER_URL + "/realms/demo/protocol/saml");
        loginPage.login("bburke", "password");
        assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/sales-post-sig-email/");
        System.out.println(driver.getPageSource());
        Assert.assertTrue(driver.getPageSource().contains("principal=bburke@redhat.com"));
        driver.navigate().to(APP_SERVER_BASE_URL + "/sales-post-sig-email?GLO=true");
        checkLoggedOut(APP_SERVER_BASE_URL + "/sales-post-sig-email/");

    }

    public void testRelayStateEncoding() throws Exception {
        // this test has a hardcoded SAMLRequest and we hack a SP face servlet to get the SAMLResponse so we can look
        // at the relay state
        SamlSPFacade.samlResponse = null;
        driver.navigate().to(APP_SERVER_BASE_URL + "/employee/");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(AUTH_SERVER_URL + "/realms/demo/protocol/saml"));
        System.out.println(driver.getCurrentUrl());
        loginPage.login("bburke", "password");
        assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/employee/");
        assertEquals(SamlSPFacade.sentRelayState, SamlSPFacade.RELAY_STATE);
        Assert.assertNotNull(SamlSPFacade.samlResponse);

    }

    public void testAttributes() throws Exception {
        {
            SendUsernameServlet.sentPrincipal = null;
            SendUsernameServlet.checkRoles = null;
            driver.navigate().to(APP_SERVER_BASE_URL + "/employee2/");
            Assert.assertTrue(driver.getCurrentUrl().startsWith(AUTH_SERVER_URL + "/realms/demo/protocol/saml"));
            List<String> requiredRoles = new LinkedList<>();
            requiredRoles.add("manager");
            requiredRoles.add("employee");
            requiredRoles.add("user");
            SendUsernameServlet.checkRoles = requiredRoles;
            loginPage.login("bburke", "password");
            assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/employee2/");
            SendUsernameServlet.checkRoles = null;
            SamlPrincipal principal = (SamlPrincipal) SendUsernameServlet.sentPrincipal;
            Assert.assertNotNull(principal);
            assertEquals("bburke@redhat.com", principal.getAttribute(X500SAMLProfileConstants.EMAIL.get()));
            assertEquals("bburke@redhat.com", principal.getFriendlyAttribute("email"));
            assertEquals("617", principal.getAttribute("phone"));
            Assert.assertNull(principal.getFriendlyAttribute("phone"));
            driver.navigate().to(APP_SERVER_BASE_URL + "/employee2/?GLO=true");
            checkLoggedOut(APP_SERVER_BASE_URL + "/employee2/");

        }
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                ClientModel app = appRealm.getClientByClientId(APP_SERVER_BASE_URL + "/employee2/");
                for (ProtocolMapperModel mapper : app.getProtocolMappers()) {
                    if (mapper.getName().equals("role-list")) {
                        app.removeProtocolMapper(mapper);
                        mapper.setId(null);
                        mapper.getConfig().put(RoleListMapper.SINGLE_ROLE_ATTRIBUTE, "true");
                        mapper.getConfig().put(AttributeStatementHelper.SAML_ATTRIBUTE_NAME, "memberOf");
                        app.addProtocolMapper(mapper);
                    }
                }
                app.addProtocolMapper(HardcodedAttributeMapper.create("hardcoded-attribute", "hardcoded-attribute", "Basic", null, "hard", false, null));
                app.addProtocolMapper(HardcodedRole.create("hardcoded-role", "hardcoded-role"));
                app.addProtocolMapper(RoleNameMapper.create("renamed-role", "manager", "el-jefe"));
                app.addProtocolMapper(RoleNameMapper.create("renamed-employee-role", APP_SERVER_BASE_URL + "/employee/.employee", "pee-on"));
            }
        }, "demo");

        System.out.println(">>>>>>>>>> single role attribute <<<<<<<<");

        {
            SendUsernameServlet.sentPrincipal = null;
            SendUsernameServlet.checkRoles = null;
            driver.navigate().to(APP_SERVER_BASE_URL + "/employee2/");
            Assert.assertTrue(driver.getCurrentUrl().startsWith(AUTH_SERVER_URL + "/realms/demo/protocol/saml"));
            List<String> requiredRoles = new LinkedList<>();
            requiredRoles.add("el-jefe");
            requiredRoles.add("user");
            requiredRoles.add("hardcoded-role");
            requiredRoles.add("pee-on");
            SendUsernameServlet.checkRoles = requiredRoles;
            loginPage.login("bburke", "password");
            assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/employee2/");
            SendUsernameServlet.checkRoles = null;
            SamlPrincipal principal = (SamlPrincipal) SendUsernameServlet.sentPrincipal;
            Assert.assertNotNull(principal);
            assertEquals("hard", principal.getAttribute("hardcoded-attribute"));


        }
    }

    public void testRedirectSignedLoginLogout() {
        driver.navigate().to(APP_SERVER_BASE_URL + "/employee-sig/");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(AUTH_SERVER_URL + "/realms/demo/protocol/saml"));
        loginPage.login("bburke", "password");
        assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/employee-sig/");
        Assert.assertTrue(driver.getPageSource().contains("bburke"));
        driver.navigate().to(APP_SERVER_BASE_URL + "/employee-sig?GLO=true");
        checkLoggedOut(APP_SERVER_BASE_URL + "/employee-sig/");

    }

    public void testRedirectSignedLoginLogoutFrontNoSSO() {
        driver.navigate().to(APP_SERVER_BASE_URL + "/employee-sig-front/");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(AUTH_SERVER_URL + "/realms/demo/protocol/saml"));
        loginPage.login("bburke", "password");
        assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/employee-sig-front/");
        Assert.assertTrue(driver.getPageSource().contains("bburke"));
        driver.navigate().to(APP_SERVER_BASE_URL + "/employee-sig-front?GLO=true");
        checkLoggedOut(APP_SERVER_BASE_URL + "/employee-sig-front/");

    }

    public void testRedirectSignedLoginLogoutFront() {
        // visit 1st app an logg in
        System.out.println("visit 1st app ");
        driver.navigate().to(APP_SERVER_BASE_URL + "/employee-sig/");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(AUTH_SERVER_URL + "/realms/demo/protocol/saml"));
        System.out.println("login to form");
        loginPage.login("bburke", "password");
        assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/employee-sig/");
        Assert.assertTrue(driver.getPageSource().contains("bburke"));

        // visit 2nd app
        System.out.println("visit 2nd app ");
        driver.navigate().to(APP_SERVER_BASE_URL + "/employee-sig-front/");
        assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/employee-sig-front/");
        Assert.assertTrue(driver.getPageSource().contains("bburke"));

        // visit 3rd app
        System.out.println("visit 3rd app ");
        driver.navigate().to(APP_SERVER_BASE_URL + "/sales-post-sig/");
        assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/sales-post-sig/");
        Assert.assertTrue(driver.getPageSource().contains("bburke"));

        // logout of first app
        System.out.println("GLO");
        driver.navigate().to(APP_SERVER_BASE_URL + "/employee-sig?GLO=true");
        checkLoggedOut(APP_SERVER_BASE_URL + "/employee-sig/");
        driver.navigate().to(APP_SERVER_BASE_URL + "/employee-sig-front/");
        String currentUrl = driver.getCurrentUrl();
        Assert.assertTrue(currentUrl.startsWith(AUTH_SERVER_URL + "/realms/demo/protocol/saml"));
        driver.navigate().to(APP_SERVER_BASE_URL + "/sales-post-sig/");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(AUTH_SERVER_URL + "/realms/demo/protocol/saml"));

    }

    public void testPostEncryptedLoginLogout() {
        driver.navigate().to(APP_SERVER_BASE_URL + "/sales-post-enc/");
        assertEquals(driver.getCurrentUrl(), AUTH_SERVER_URL + "/realms/demo/protocol/saml");
        loginPage.login("bburke", "password");
        assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/sales-post-enc/");
        Assert.assertTrue(driver.getPageSource().contains("bburke"));
        driver.navigate().to(APP_SERVER_BASE_URL + "/sales-post-enc?GLO=true");
        checkLoggedOut(APP_SERVER_BASE_URL + "/sales-post-enc/");

    }
    public void testPostBadClientSignature() {
        driver.navigate().to(APP_SERVER_BASE_URL + "/bad-client-sales-post-sig/");
        assertEquals(driver.getCurrentUrl(), AUTH_SERVER_URL + "/realms/demo/protocol/saml");
        assertEquals(driver.getTitle(), "We're sorry...");

    }
    public static interface CheckAuthError {
        void check(WebDriver driver);
    }

    public void testPostBadRealmSignature(CheckAuthError error) {
        driver.navigate().to(APP_SERVER_BASE_URL + "/bad-realm-sales-post-sig/");
        assertEquals(driver.getCurrentUrl(), AUTH_SERVER_URL + "/realms/demo/protocol/saml");
        loginPage.login("bburke", "password");
        assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/bad-realm-sales-post-sig/");
        System.out.println(driver.getPageSource());
        error.check(driver);
    }

    public void testMetadataPostSignedLoginLogout() throws Exception {

        driver.navigate().to(APP_SERVER_BASE_URL + "/sales-metadata/");
        assertEquals(driver.getCurrentUrl(), AUTH_SERVER_URL + "/realms/demo/protocol/saml");
        loginPage.login("bburke", "password");
        assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/sales-metadata/");
        String pageSource = driver.getPageSource();
        Assert.assertTrue(pageSource.contains("bburke"));
        driver.navigate().to(APP_SERVER_BASE_URL + "/sales-metadata?GLO=true");
        checkLoggedOut(APP_SERVER_BASE_URL + "/sales-metadata/");

    }

    public static void uploadSP(String AUTH_SERVER_URL) {
        try {
            Keycloak keycloak = Keycloak.getInstance(AUTH_SERVER_URL, "master", "admin", "admin", Constants.ADMIN_CONSOLE_CLIENT_ID, null);
            RealmResource admin = keycloak.realm("demo");

            admin.toRepresentation();

            ClientRepresentation clientRep = admin.convertClientDescription(IOUtils.toString(SamlAdapterTestStrategy.class.getResourceAsStream("/keycloak-saml/sp-metadata.xml")));
            Response response = admin.clients().create(clientRep);

            assertEquals(201, response.getStatus());

            keycloak.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
