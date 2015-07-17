package org.keycloak.testsuite.saml;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.Config;
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
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.api.saml.v2.response.SAML2Response;
import org.keycloak.saml.processing.core.saml.v2.constants.X500SAMLProfileConstants;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.saml.processing.web.util.PostBindingUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import java.io.ByteArrayInputStream;
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
            //initializeSamlSecuredWar("/saml/simple-get", "/employee",  "employee.war", classLoader);
            initializeSamlSecuredWar("/saml/signed-front-get", "/employee-sig-front",  "employee-sig-front.war", classLoader);
            initializeSamlSecuredWar("/saml/bad-client-signed-post", "/bad-client-sales-post-sig",  "bad-client-post-sig.war", classLoader);
            initializeSamlSecuredWar("/saml/bad-realm-signed-post", "/bad-realm-sales-post-sig",  "bad-realm-post-sig.war", classLoader);
            initializeSamlSecuredWar("/saml/encrypted-post", "/sales-post-enc",  "post-enc.war", classLoader);
            uploadSP();
            server.getServer().deploy(createDeploymentInfo("employee.war", "/employee", SamlSPFacade.class));



        }

        @Override
        public String getRealmJson() {
            return "/saml/testsaml.json";
        }
    };

    public static class SamlSPFacade extends HttpServlet {
        public static String samlResponse;
        public static String RELAY_STATE = "http://test.com/foo/bar";
        public static String sentRelayState;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            handler(req, resp);
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            handler(req, resp);
        }

        private void handler(HttpServletRequest req, HttpServletResponse resp) {
            System.out.println("********* HERE ******");
            if (req.getParameterMap().isEmpty()) {
                System.out.println("redirecting");
                resp.setStatus(302);
                // Redirect
                // UriBuilder builder = UriBuilder.fromUri("http://localhost:8081/auth/realms/demo/protocol/saml?SAMLRequest=jVLRTsIwFP2Vpe%2BjG4wxG0YyWYxL0BBAH3wx3XYnTbp29nYof%2B8YEvEBNOlD03vOveec2ynyWjYsae1WreC9BbTOZy0Vsr4Qk9YopjkKZIrXgMwWbJ08LNhw4LHGaKsLLcmRch3MEcFYoRVxktN1rhW2NZg1mJ0o4Gm1iMnW2oZRKnXB5VajZZEX%2BRTqRuo9ACVO2mkUih%2F4l9C8s0MNcFkjLaHW9KSUHlwR506bAnrPMam4RCBOlsYkS1%2BD3MvLcDJxAx9KN4jCkXszrG5cP%2BCVH4y8IM8PYFx2dsQOfuiILWQKLVc2JkPPH7te6HrRxh%2BzUdidwSSIXoiz%2FBZyK1Qp1Nv1yPIjCNn9ZrN0V1AKA4UlzjMY7N13IDKbHjyxXoA5291%2FtzH7I%2FApPet%2FHNawx65hli61FMXeSaTUH%2FMubtvlYU0LfcA1t5cl%2BAO%2FfxGlW%2FVQ1ipsoBCVgJLQ2XHo7385%2BwI%3D");
                UriBuilder builder = UriBuilder.fromUri("http://localhost:8081/auth/realms/demo/protocol/saml?SAMLRequest=jVJbT8IwFP4rS99HuwluNIwEIUYSLwugD76Y2h2kSdfOng7l31uGRn0ATfrQ9HznfJfTEYpaN3zS%2Bo1ZwGsL6KP3WhvkXaEgrTPcClTIjagBuZd8Obm55mmP8cZZb6XV5NByGiwQwXllDYkmX9epNdjW4JbgtkrC%2FeK6IBvvG06ptlLojUXPc5YnFOpG2x0AJdEsaFRG7PuPoUWwQx0IXSOtoLb0SynduyLRpXUSOs8FWQuNQKL5rCDz2VO%2FymEgIY2zlJ3H%2FSx9jkU%2BzOK0ys8yNmSSsUEAYxnsqC18tyO2MDfohfEFSVkyiNlZzM5XacrDSbJePug%2Fkqj8FHKhTKXMy%2BnIng8g5FerVRmXd8sViR7AYec8AMh4tPfDO3L3Y2%2F%2F3cT4j7BH9Mf8A1nDb8PA%2Bay0WsldNNHavk1D1D5k4V0LXbi18MclJL2ke1FVvO6gvDXYgFRrBRWh4wPp7z85%2FgA%3D");
                builder.queryParam("RelayState", RELAY_STATE);
                resp.setHeader("Location", builder.build().toString());
                return;
            }
            System.out.println("received response");
            samlResponse = req.getParameter("SAMLResponse");
            sentRelayState = req.getParameter("RelayState");
        }
    }

    @Rule
    public WebRule webRule = new WebRule(this);
    @WebResource
    protected WebDriver driver;
    @WebResource
    protected LoginPage loginPage;

    protected void checkLoggedOut(String mainUrl) {
        String pageSource = driver.getPageSource();
        System.out.println("*** logout pagesouce ***");
        System.out.println(pageSource);
        System.out.println("driver url: " + driver.getCurrentUrl());
        Assert.assertTrue(pageSource.contains("request-path: /logout.jsp"));
        driver.navigate().to(mainUrl);
        Assert.assertTrue(driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/demo/protocol/saml"));
    }

    //@Test
    public void ideTesting() throws Exception {
        Thread.sleep(100000000);
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
    public void testPostSimpleLoginLogoutIdpInitiated() {
        driver.navigate().to("http://localhost:8081/auth/realms/demo/protocol/saml/clients/sales-post");
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
    public void testRelayStateEncoding() throws Exception {
        // this test has a hardcoded SAMLRequest and we hack a SP face servlet to get the SAMLResponse so we can look
        // at the relay state
        SamlSPFacade.samlResponse = null;
        driver.navigate().to("http://localhost:8081/employee/");
        Assert.assertTrue(driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/demo/protocol/saml"));
        System.out.println(driver.getCurrentUrl());
        loginPage.login("bburke", "password");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/employee/");
        Assert.assertEquals(SamlSPFacade.sentRelayState, SamlSPFacade.RELAY_STATE);
        Assert.assertNotNull(SamlSPFacade.samlResponse);

    }


    @Test
    public void testAttributes() throws Exception {
        // this test has a hardcoded SAMLRequest and we hack a SP face servlet to get the SAMLResponse so we can look
        // at the assertions sent.  This is because Picketlink, AFAICT, does not give you any way to get access to
        // the assertion.

        {
            SamlSPFacade.samlResponse = null;
            driver.navigate().to("http://localhost:8081/employee/");
            Assert.assertTrue(driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/demo/protocol/saml"));
            System.out.println(driver.getCurrentUrl());
            loginPage.login("bburke", "password");
            Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/employee/");
            Assert.assertNotNull(SamlSPFacade.samlResponse);
            SAML2Response saml2Response = new SAML2Response();
            byte[] samlResponse = PostBindingUtil.base64Decode(SamlSPFacade.samlResponse);
            ResponseType rt = saml2Response.getResponseType(new ByteArrayInputStream(samlResponse));
            Assert.assertTrue(rt.getAssertions().size() == 1);
            AssertionType assertion = rt.getAssertions().get(0).getAssertion();

            // test attributes and roles

            boolean email = false;
            boolean phone = false;
            boolean userRole = false;
            boolean managerRole = false;
            for (AttributeStatementType statement : assertion.getAttributeStatements()) {
                for (AttributeStatementType.ASTChoiceType choice : statement.getAttributes()) {
                    AttributeType attr = choice.getAttribute();
                    if (X500SAMLProfileConstants.EMAIL.getFriendlyName().equals(attr.getFriendlyName())) {
                        Assert.assertEquals(X500SAMLProfileConstants.EMAIL.get(), attr.getName());
                        Assert.assertEquals(JBossSAMLURIConstants.ATTRIBUTE_FORMAT_URI.get(), attr.getNameFormat());
                        Assert.assertEquals(attr.getAttributeValue().get(0), "bburke@redhat.com");
                        email = true;
                    } else if (attr.getName().equals("phone")) {
                        Assert.assertEquals(JBossSAMLURIConstants.ATTRIBUTE_FORMAT_BASIC.get(), attr.getNameFormat());
                        Assert.assertEquals(attr.getAttributeValue().get(0), "617");
                        phone = true;
                    } else if (attr.getName().equals("Role")) {
                        if (attr.getAttributeValue().get(0).equals("manager")) managerRole = true;
                        if (attr.getAttributeValue().get(0).equals("user")) userRole = true;
                    }
                }

            }

            Assert.assertTrue(email);
            Assert.assertTrue(phone);
            Assert.assertTrue(userRole);
            Assert.assertTrue(managerRole);
        }

        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                ClientModel app = appRealm.getClientByClientId("http://localhost:8081/employee/");
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
                app.addProtocolMapper(RoleNameMapper.create("renamed-employee-role", "http://localhost:8081/employee/.employee", "pee-on"));
            }
        }, "demo");

        System.out.println(">>>>>>>>>> single role attribute <<<<<<<<");

        {
            SamlSPFacade.samlResponse = null;
            driver.navigate().to("http://localhost:8081/employee/");
            System.out.println(driver.getCurrentUrl());
            Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/employee/");
            Assert.assertNotNull(SamlSPFacade.samlResponse);
            SAML2Response saml2Response = new SAML2Response();
            byte[] samlResponse = PostBindingUtil.base64Decode(SamlSPFacade.samlResponse);
            ResponseType rt = saml2Response.getResponseType(new ByteArrayInputStream(samlResponse));
            Assert.assertTrue(rt.getAssertions().size() == 1);
            AssertionType assertion = rt.getAssertions().get(0).getAssertion();

            // test attributes and roles

            boolean userRole = false;
            boolean managerRole = false;
            boolean single = false;
            boolean hardcodedRole = false;
            boolean hardcodedAttribute = false;
            boolean peeOn = false;
            for (AttributeStatementType statement : assertion.getAttributeStatements()) {
                for (AttributeStatementType.ASTChoiceType choice : statement.getAttributes()) {
                    AttributeType attr = choice.getAttribute();
                    if (attr.getName().equals("memberOf")) {
                        if (single) Assert.fail("too many role attributes");
                        single = true;
                        for (Object value : attr.getAttributeValue()) {
                            if (value.equals("el-jefe")) managerRole = true;
                            if (value.equals("user")) userRole = true;
                            if (value.equals("hardcoded-role")) hardcodedRole = true;
                            if (value.equals("pee-on")) peeOn = true;
                        }
                    } else if (attr.getName().equals("hardcoded-attribute")) {
                        hardcodedAttribute = true;
                        Assert.assertEquals(attr.getAttributeValue().get(0), "hard");
                    }
                }

            }

            Assert.assertTrue(single);
            Assert.assertTrue(hardcodedAttribute);
            Assert.assertTrue(hardcodedRole);
            Assert.assertTrue(peeOn);
            Assert.assertTrue(userRole);
            Assert.assertTrue(managerRole);
        }
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

        // visit 3rd app
        System.out.println("visit 3rd app ");
        driver.navigate().to("http://localhost:8081/sales-post-sig/");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/sales-post-sig/");
        Assert.assertTrue(driver.getPageSource().contains("bburke"));

        // logout of first app
        System.out.println("GLO");
        driver.navigate().to("http://localhost:8081/employee-sig?GLO=true");
        checkLoggedOut("http://localhost:8081/employee-sig/");
        driver.navigate().to("http://localhost:8081/employee-sig-front/");
        Assert.assertTrue(driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/demo/protocol/saml"));
        driver.navigate().to("http://localhost:8081/sales-post-sig/");
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
            ClientModel adminConsole = adminRealm.getClientByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID);
            TokenManager tm = new TokenManager();
            UserModel admin = session.users().getUserByUsername("admin", adminRealm);
            ClientSessionModel clientSession = session.sessions().createClientSession(adminRealm, adminConsole);
            clientSession.setNote(OIDCLoginProtocol.ISSUER, "http://localhost:8081/auth/realms/master");
            UserSessionModel userSession = session.sessions().createUserSession(adminRealm, admin, "admin", null, "form", false, null, null);
            AccessToken token = tm.createClientAccessToken(session, tm.getAccess(null, adminConsole, admin), adminRealm, adminConsole, admin, userSession, clientSession);
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

        WebTarget upload = adminRealms.path("demo/client-importers/saml2-entity-descriptor/upload");
        System.out.println(upload.getUri());
        Response response = upload.request().post(Entity.entity(formData, MediaType.MULTIPART_FORM_DATA));
        Assert.assertEquals(204, response.getStatus());
        response.close();
        client.close();
    }


}
