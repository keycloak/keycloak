package org.keycloak.testsuite.adapter.servlet;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import jakarta.ws.rs.core.Response.Status;

import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.adapter.page.EmployeeServlet;
import org.keycloak.testsuite.adapter.page.SalesPostServlet;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.testsuite.util.saml.CreateLogoutRequestStepBuilder;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;
import org.keycloak.testsuite.utils.io.IOUtil;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;

import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.samlServletDeployment;
import static org.keycloak.testsuite.adapter.servlet.SAMLServletAdapterTest.FORBIDDEN_TEXT;
import static org.keycloak.testsuite.adapter.servlet.SAMLServletAdapterTest.WEBSPHERE_FORBIDDEN_TEXT;
import static org.keycloak.testsuite.saml.AbstractSamlTest.SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY;
import static org.keycloak.testsuite.saml.AbstractSamlTest.SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY;
import static org.keycloak.testsuite.util.Matchers.isSamlResponse;
import static org.keycloak.testsuite.util.SamlClient.Binding.POST;
import static org.keycloak.testsuite.util.SamlClient.Binding.REDIRECT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 *
 * @author hmlnarik
 */
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP8)
public class SAMLLogoutAdapterTest extends AbstractServletsAdapterTest {

    private static final String SP_PROVIDED_ID = "spProvidedId";
    private static final String SP_NAME_QUALIFIER = "spNameQualifier";
    private static final String NAME_QUALIFIER = "nameQualifier";

    @Deployment(name = EmployeeServlet.DEPLOYMENT_NAME)
    protected static WebArchive employee() {
        return samlServletDeployment(EmployeeServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Deployment(name = SalesPostServlet.DEPLOYMENT_NAME)
    protected static WebArchive sales() {
        return samlServletDeployment(SalesPostServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Page
    private EmployeeServlet employeeServletPage;

    @Page
    private SalesPostServlet salesPostServlet;

    private final AtomicReference<NameIDType> nameIdRef = new AtomicReference<>();
    private final AtomicReference<String> sessionIndexRef = new AtomicReference<>();

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(IOUtil.loadRealm("/adapter-test/keycloak-saml/testsaml.json"));
    }

    @Override
    protected boolean isImportAfterEachMethod() {
        return false;
    }

    private SAML2Object extractNameId(SAML2Object so) {
        assertThat(so, isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        ResponseType loginResp1 = (ResponseType) so;
        final AssertionType firstAssertion = loginResp1.getAssertions().get(0).getAssertion();
        assertThat(firstAssertion, org.hamcrest.Matchers.notNullValue());
        assertThat(firstAssertion.getSubject().getSubType().getBaseID(), instanceOf(NameIDType.class));

        NameIDType nameId = (NameIDType) firstAssertion.getSubject().getSubType().getBaseID();
        AuthnStatementType firstAssertionStatement = (AuthnStatementType) firstAssertion.getStatements().iterator().next();

        nameIdRef.set(nameId);
        sessionIndexRef.set(firstAssertionStatement.getSessionIndex());

        return so;
    }

    @Test
    public void employeeGlobalLogoutTest() {
        SAMLDocumentHolder b = new SamlClientBuilder()
          .navigateTo(employeeServletPage)
          .processSamlResponse(Binding.POST)
            .build()
          .login().user(bburkeUser).build()
          .processSamlResponse(Binding.POST)
            .targetAttributeSamlResponse()
            .transformObject(this::extractNameId)
            .transformObject((SAML2Object o) -> {
              assertThat(o, isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
              ResponseType rt = (ResponseType) o;
              NameIDType t = (NameIDType) rt.getAssertions().get(0).getAssertion().getSubject().getSubType().getBaseID();
              t.setNameQualifier(NAME_QUALIFIER);
              t.setSPNameQualifier(SP_NAME_QUALIFIER);
              t.setSPProvidedID(SP_PROVIDED_ID);
            }).build()
          .navigateTo(employeeServletPage.getUriBuilder().clone().queryParam("GLO", "true").build())
          .getSamlResponse(Binding.POST);
        
        assertThat(b.getSamlObject(), instanceOf(LogoutRequestType.class));
        LogoutRequestType lr = (LogoutRequestType) b.getSamlObject();
        NameIDType logoutRequestNameID = lr.getNameID();
        assertThat(logoutRequestNameID.getFormat(), is(nameIdRef.get().getFormat()));
        assertThat(logoutRequestNameID.getValue(), is(nameIdRef.get().getValue()));
        assertThat(logoutRequestNameID.getNameQualifier(), is(NAME_QUALIFIER));
        assertThat(logoutRequestNameID.getSPProvidedID(), is(SP_PROVIDED_ID));
        assertThat(logoutRequestNameID.getSPNameQualifier(), is(SP_NAME_QUALIFIER));
    }

    @Test
    public void testLogoutDestinationOptionalIfUnsignedRedirect() throws IOException {
        testLogoutDestination(REDIRECT,
          builder -> builder.transformObject(logoutReq -> { logoutReq.setDestination(null); }),
          SAMLLogoutAdapterTest::assertSamlLogoutResponse
        );
    }

    @Test
    public void testLogoutMandatoryDestinationUnsetRedirect() throws IOException {
        testLogoutDestination(REDIRECT,
          builder -> builder
                       .transformObject(logoutReq -> { logoutReq.setDestination(null); })
                       .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY),
          SAMLLogoutAdapterTest::assertBadRequest
        );
    }

    @Test
    public void testLogoutMandatoryDestinationSetRedirect() throws IOException {
        testLogoutDestination(REDIRECT,
          builder -> builder.signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY),
          SAMLLogoutAdapterTest::assertSamlLogoutResponse
        );
    }

    @Test
    public void testLogoutDestinationOptionalIfUnsignedPost() throws IOException {
        testLogoutDestination(POST,
          builder -> builder.transformObject(logoutReq -> { logoutReq.setDestination(null); }),
          SAMLLogoutAdapterTest::assertSamlLogoutResponse
        );
    }

    @Test
    public void testLogoutMandatoryDestinationUnsetPost() throws IOException {
        testLogoutDestination(POST,
          builder -> builder
                       .transformObject(logoutReq -> { logoutReq.setDestination(null); })
                       .signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY),
          SAMLLogoutAdapterTest::assertBadRequest
        );
    }

    @Test
    public void testLogoutMandatoryDestinationSetPost() throws IOException {
        testLogoutDestination(POST,
          builder -> builder.signWith(SAML_CLIENT_SALES_POST_SIG_PRIVATE_KEY, SAML_CLIENT_SALES_POST_SIG_PUBLIC_KEY),
          SAMLLogoutAdapterTest::assertSamlLogoutResponse
        );
    }

    private void testLogoutDestination(Binding binding, final Consumer<CreateLogoutRequestStepBuilder> logoutReqUpdater, Consumer<? super CloseableHttpResponse> responseTester) throws IOException {
        URI clientSamlEndpoint = salesPostServlet.getUriBuilder().clone().path("saml").build();

        new SamlClientBuilder()
          .navigateTo(salesPostServlet)
          .processSamlResponse(Binding.POST)
            .build()
          .login().user(bburkeUser).build()

          .processSamlResponse(Binding.POST)
            .targetAttributeSamlResponse()
            .transformObject(this::extractNameId)
            .build()

          .logoutRequest(clientSamlEndpoint, "http://no.one.cares/", binding)
            .nameId(nameIdRef::get)
            .sessionIndex(sessionIndexRef::get)
            .apply(logoutReqUpdater)
            .build()

          .doNotFollowRedirects()
          .assertResponse(responseTester)

          .execute();
    }

    public static void assertSamlLogoutResponse(CloseableHttpResponse response) {
        try {
            assertThat(POST.extractResponse(response).getSamlObject(), Matchers.isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void assertBadRequest(HttpResponse response) {
        assertThat(response, anyOf(
          Matchers.statusCodeIsHC(Status.BAD_REQUEST),
          Matchers.statusCodeIsHC(Status.FORBIDDEN),
          Matchers.bodyHC(anyOf(
            containsString("Forbidden"),
            containsString(FORBIDDEN_TEXT),
            containsString(WEBSPHERE_FORBIDDEN_TEXT)
          ))
        ));
    }
}
