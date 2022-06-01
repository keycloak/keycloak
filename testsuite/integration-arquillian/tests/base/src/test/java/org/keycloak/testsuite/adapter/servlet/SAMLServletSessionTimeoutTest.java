package org.keycloak.testsuite.adapter.servlet;

import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.keycloak.adapters.rotation.PublicKeyLocator;
import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.StatementAbstractType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.models.utils.SessionTimeoutHelper;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.keycloak.testsuite.adapter.filter.AdapterActionsFilter;
import org.keycloak.testsuite.adapter.page.Employee2Servlet;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.saml.AbstractSamlTest.REALM_NAME;
import static org.keycloak.testsuite.util.Matchers.bodyHC;


@AppServerContainer(ContainerConstants.APP_SERVER_UNDERTOW)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP6)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP71)
@AppServerContainer(ContainerConstants.APP_SERVER_TOMCAT8)
@AppServerContainer(ContainerConstants.APP_SERVER_TOMCAT9)
@AppServerContainer(ContainerConstants.APP_SERVER_JETTY94)
public class SAMLServletSessionTimeoutTest extends AbstractSAMLServletAdapterTest {

    @Page
    protected Employee2Servlet employee2ServletPage;

    @Deployment(name = Employee2Servlet.DEPLOYMENT_NAME)
    protected static WebArchive employee2() {
        return samlServletDeployment(Employee2Servlet.DEPLOYMENT_NAME, WEB_XML_WITH_ACTION_FILTER, SendUsernameServlet.class, AdapterActionsFilter.class, PublicKeyLocator.class);
    }

    private static final int SESSION_LENGTH_IN_SECONDS = 120;
    private static final int KEYCLOAK_SESSION_TIMEOUT = 1922; /** 1800 session max + 120 {@link SessionTimeoutHelper#IDLE_TIMEOUT_WINDOW_SECONDS}  */

    private AtomicReference<String> sessionNotOnOrAfter = new AtomicReference<>();

    private SAML2Object addSessionNotOnOrAfter(SAML2Object ob) {
        assertThat(ob, Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        ResponseType resp = (ResponseType) ob;

        Set<StatementAbstractType> statements = resp.getAssertions().get(0).getAssertion().getStatements();

        AuthnStatementType authType = (AuthnStatementType) statements.stream()
                .filter(statement -> statement instanceof AuthnStatementType)
                .findFirst().orElse(new AuthnStatementType(XMLTimeUtil.getIssueInstant()));
        XMLGregorianCalendar sessionTimeout = XMLTimeUtil.add(XMLTimeUtil.getIssueInstant(), SESSION_LENGTH_IN_SECONDS * 1000);
        sessionNotOnOrAfter.set(sessionTimeout.toString());
        authType.setSessionNotOnOrAfter(sessionTimeout);
        resp.getAssertions().get(0).getAssertion().addStatement(authType);

        return ob;
    }

    @Test
    public void employee2TestSAMLRefreshingSession() {
        sessionNotOnOrAfter.set(null);

        beginAuthenticationAndLogin(employee2ServletPage, SamlClient.Binding.POST)
                .processSamlResponse(SamlClient.Binding.POST) // Update response with SessionNotOnOrAfter
                    .transformObject(this::addSessionNotOnOrAfter)
                    .build()
                .addStep(() -> setAdapterAndServerTimeOffset(100, employee2ServletPage.toString())) // Move in time right before sessionNotOnOrAfter
                .navigateTo(employee2ServletPage.buildUri())
                .assertResponse(response -> // Check that session is still valid within sessionTimeout limit
                        assertThat(response, // Cannot use matcher as sessionNotOnOrAfter variable is not set in time of creating matcher
                                bodyHC(allOf(containsString("principal=bburke"),
                                        containsString("SessionNotOnOrAfter: " + sessionNotOnOrAfter.get())))))
                .addStep(() -> setAdapterAndServerTimeOffset(SESSION_LENGTH_IN_SECONDS, employee2ServletPage.toString())) // Move in time after sessionNotOnOrAfter
                .navigateTo(employee2ServletPage.buildUri())
                .processSamlResponse(SamlClient.Binding.POST) // AuthnRequest should be send
                    .transformObject(ob -> {
                        assertThat(ob, Matchers.isSamlAuthnRequest());
                        return ob;
                    })
                    .build()

                .followOneRedirect() // There is a redirect on Keycloak side
                .processSamlResponse(SamlClient.Binding.POST) // Process the response from keyclok, no login form should be present since session on keycloak side is still valid
                    .build()
                .assertResponse(bodyHC(containsString("principal=bburke")))
                .execute();

                setAdapterAndServerTimeOffset(0, employee2ServletPage.toString());
    }

    @Test
    public void employee2TestSAMLSessionTimeoutOnBothSides() {
        sessionNotOnOrAfter.set(null);

        beginAuthenticationAndLogin(employee2ServletPage, SamlClient.Binding.POST)
                .processSamlResponse(SamlClient.Binding.POST) // Update response with SessionNotOnOrAfter
                    .transformObject(this::addSessionNotOnOrAfter)
                    .build()

                .navigateTo(employee2ServletPage.buildUri())
                .assertResponse(response -> // Check that session is still valid within sessionTimeout limit
                        assertThat(response, // Cannot use matcher as sessionNotOnOrAfter variable is not set in time of creating matcher
                                bodyHC(allOf(containsString("principal=bburke"),
                                        containsString("SessionNotOnOrAfter: " + sessionNotOnOrAfter.get())))))
                .addStep(() -> setAdapterAndServerTimeOffset(KEYCLOAK_SESSION_TIMEOUT, employee2ServletPage.toString())) // Move in time after sessionNotOnOrAfter and keycloak session
                .navigateTo(employee2ServletPage.buildUri())
                .processSamlResponse(SamlClient.Binding.POST) // AuthnRequest should be send
                    .transformObject(ob -> {
                        assertThat(ob, Matchers.isSamlAuthnRequest());
                        return ob;
                    })
                    .build()

                .followOneRedirect() // There is a redirect on Keycloak side
                .assertResponse(Matchers.bodyHC(containsString("form id=\"kc-form-login\"")))
                .execute();

        setAdapterAndServerTimeOffset(0, employee2ServletPage.toString());
    }

    @Test
    public void testKeycloakReturnsSessionNotOnOrAfter() throws Exception {
        sessionNotOnOrAfter.set(null);

        try(AutoCloseable c = new RealmAttributeUpdater(adminClient.realm(REALM_NAME))
                .updateWith(r -> r.setSsoSessionMaxLifespan(SESSION_LENGTH_IN_SECONDS))
                .update()) {
            beginAuthenticationAndLogin(employee2ServletPage, SamlClient.Binding.POST)
                    .processSamlResponse(SamlClient.Binding.POST) // Process response
                        .transformObject(ob -> { // Check sessionNotOnOrAfter is present and it has correct value
                            assertThat(ob, Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
                            ResponseType resp = (ResponseType) ob;

                            Set<StatementAbstractType> statements = resp.getAssertions().get(0).getAssertion().getStatements();

                            AuthnStatementType authType = (AuthnStatementType) statements.stream()
                                    .filter(statement -> statement instanceof AuthnStatementType)
                                    .findFirst().orElseThrow(() -> new RuntimeException("SamlReponse doesn't contain AuthStatement"));

                            assertThat(authType.getSessionNotOnOrAfter(), notNullValue());
                            XMLGregorianCalendar expectedSessionTimeout = XMLTimeUtil.add(authType.getAuthnInstant(), SESSION_LENGTH_IN_SECONDS * 1000);
                            assertThat(authType.getSessionNotOnOrAfter(), is(expectedSessionTimeout));
                            sessionNotOnOrAfter.set(expectedSessionTimeout.toString());

                            return ob;
                        })
                        .build()

                    .navigateTo(employee2ServletPage.buildUri())
                    .assertResponse(response -> // Check that session is still valid within sessionTimeout limit
                            assertThat(response, // Cannot use matcher as sessionNotOnOrAfter variable is not set in time of creating matcher
                                    bodyHC(allOf(containsString("principal=bburke"),
                                            containsString("SessionNotOnOrAfter: " + sessionNotOnOrAfter.get())))))
                    .addStep(() -> setAdapterAndServerTimeOffset(KEYCLOAK_SESSION_TIMEOUT, employee2ServletPage.toString())) // Move in time after sessionNotOnOrAfter and keycloak session
                    .navigateTo(employee2ServletPage.buildUri())
                    .processSamlResponse(SamlClient.Binding.POST) // AuthnRequest should be send
                    .transformObject(ob -> {
                        assertThat(ob, Matchers.isSamlAuthnRequest());
                        return ob;
                    })
                    .build()

                    .followOneRedirect() // There is a redirect on Keycloak side
                    .assertResponse(Matchers.bodyHC(containsString("form id=\"kc-form-login\"")))
                    .execute();

            setAdapterAndServerTimeOffset(0, employee2ServletPage.toString());
        }
    }
}
