package org.keycloak.testsuite.adapter.servlet;

import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.adapter.page.EmployeeServlet;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;
import org.keycloak.testsuite.utils.io.IOUtil;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.Matchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.samlServletDeployment;
import static org.keycloak.testsuite.util.Matchers.isSamlResponse;

/**
 *
 * @author hmlnarik
 */
@AppServerContainer(ContainerConstants.APP_SERVER_UNDERTOW)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY_DEPRECATED)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP6)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP71)
@AppServerContainer(ContainerConstants.APP_SERVER_TOMCAT7)
@AppServerContainer(ContainerConstants.APP_SERVER_TOMCAT8)
@AppServerContainer(ContainerConstants.APP_SERVER_TOMCAT9)
public class SAMLLogoutAdapterTest extends AbstractServletsAdapterTest {

    private static final String SP_PROVIDED_ID = "spProvidedId";
    private static final String SP_NAME_QUALIFIER = "spNameQualifier";
    private static final String NAME_QUALIFIER = "nameQualifier";

    @Deployment(name = EmployeeServlet.DEPLOYMENT_NAME)
    protected static WebArchive employee() {
        return samlServletDeployment(EmployeeServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Page
    private EmployeeServlet employeeServletPage;

    private final AtomicReference<NameIDType> nameIdRef = new AtomicReference<>();

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(IOUtil.loadRealm("/adapter-test/keycloak-saml/testsaml.json"));
    }

    private SAML2Object extractNameId(SAML2Object so) {
        assertThat(so, isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        ResponseType loginResp1 = (ResponseType) so;
        final AssertionType firstAssertion = loginResp1.getAssertions().get(0).getAssertion();
        assertThat(firstAssertion, org.hamcrest.Matchers.notNullValue());
        assertThat(firstAssertion.getSubject().getSubType().getBaseID(), instanceOf(NameIDType.class));

        NameIDType nameId = (NameIDType) firstAssertion.getSubject().getSubType().getBaseID();

        nameIdRef.set(nameId);

        return so;
    }

    @Test
    public void employeeTest() {
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
              return o;
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

}
