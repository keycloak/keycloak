package org.keycloak.testsuite.saml;

import java.util.List;
import org.junit.Test;
import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.StatementAbstractType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;

import java.util.Set;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.Assert;
import static org.junit.Assert.assertThat;
import org.keycloak.dom.saml.v2.assertion.ConditionsType;
import org.keycloak.dom.saml.v2.assertion.SubjectConfirmationDataType;
import org.keycloak.dom.saml.v2.assertion.SubjectConfirmationType;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;

/**
 * @author mhajas
 */
@AuthServerContainerExclude(AuthServerContainerExclude.AuthServer.REMOTE)
public class SessionNotOnOrAfterTest extends AbstractSamlTest {

    private static final int SSO_MAX_LIFESPAN = 3602;
    private static final int ACCESS_CODE_LIFESPAN = 600;
    private static final int ACCESS_TOKEN_LIFESPAN = 1200;

    private SAML2Object checkSessionNotOnOrAfter(SAML2Object ob, int ssoMaxLifespan,
            int accessCodeLifespan, int accessTokenLifespan) {
        assertThat(ob, Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        ResponseType resp = (ResponseType) ob;

        Assert.assertNotNull(resp);
        Assert.assertNotNull(resp.getAssertions());
        Assert.assertThat(resp.getAssertions().size(), greaterThan(0));
        Assert.assertNotNull(resp.getAssertions().get(0));
        Assert.assertNotNull(resp.getAssertions().get(0).getAssertion());

        // session lifespan
        Assert.assertNotNull(resp.getAssertions().get(0).getAssertion().getStatements());
        Set<StatementAbstractType> statements = resp.getAssertions().get(0).getAssertion().getStatements();
        AuthnStatementType authType = statements.stream()
                .filter(statement -> statement instanceof AuthnStatementType)
                .map(s -> (AuthnStatementType) s)
                .findFirst().orElse(null);

        assertThat(authType, notNullValue());
        assertThat(authType.getSessionNotOnOrAfter(), notNullValue());
        assertThat(authType.getSessionNotOnOrAfter(), is(XMLTimeUtil.add(authType.getAuthnInstant(), ssoMaxLifespan * 1000L)));

        // Conditions
        Assert.assertNotNull(resp.getAssertions().get(0).getAssertion().getConditions());
        Assert.assertNotNull(resp.getAssertions().get(0).getAssertion().getConditions());
        ConditionsType condition = resp.getAssertions().get(0).getAssertion().getConditions();

        Assert.assertEquals(XMLTimeUtil.add(condition.getNotBefore(), accessCodeLifespan * 1000L), condition.getNotOnOrAfter());

        // SubjectConfirmation (confirmationData has no NotBefore, using the previous one because it's the same)
        Assert.assertNotNull(resp.getAssertions().get(0).getAssertion().getSubject());
        Assert.assertNotNull(resp.getAssertions().get(0).getAssertion().getSubject().getConfirmation());
        List<SubjectConfirmationType> confirmations = resp.getAssertions().get(0).getAssertion().getSubject().getConfirmation();

        SubjectConfirmationDataType confirmationData = confirmations.stream()
                .map(c -> c.getSubjectConfirmationData())
                .filter(c -> c != null)
                .findFirst()
                .orElse(null);

        Assert.assertNotNull(confirmationData);
        Assert.assertEquals(XMLTimeUtil.add(condition.getNotBefore(), accessTokenLifespan * 1000L), confirmationData.getNotOnOrAfter());

        return null;
    }

    @Test
    public void testSamlResponseContainsSessionNotOnOrAfterIdpInitiatedLogin() throws Exception {
        try(AutoCloseable c = new RealmAttributeUpdater(adminClient.realm(REALM_NAME))
            .updateWith(r -> {
                r.setSsoSessionMaxLifespan(SSO_MAX_LIFESPAN);
                r.setAccessCodeLifespan(ACCESS_CODE_LIFESPAN);
                r.setAccessTokenLifespan(ACCESS_TOKEN_LIFESPAN);
            })
            .update()) {
             new SamlClientBuilder()
                    .idpInitiatedLogin(getAuthServerSamlEndpoint(REALM_NAME), "sales-post").build()
                    .login().user(bburkeUser).build()
                    .processSamlResponse(SamlClient.Binding.POST)
                        .transformObject(r -> { return checkSessionNotOnOrAfter(r, SSO_MAX_LIFESPAN, ACCESS_CODE_LIFESPAN, ACCESS_TOKEN_LIFESPAN); })
                        .build()
                    .execute();
        }
    }

    @Test
    public void testMaxValuesForAllTimeouts() throws Exception {
        try(AutoCloseable c = new RealmAttributeUpdater(adminClient.realm(REALM_NAME))
                .updateWith(r -> {
                    r.setSsoSessionMaxLifespan(Integer.MAX_VALUE);
                    r.setAccessCodeLifespan(Integer.MAX_VALUE);
                    r.setAccessTokenLifespan(Integer.MAX_VALUE);
                })
                .update()) {
            new SamlClientBuilder()
                    .idpInitiatedLogin(getAuthServerSamlEndpoint(REALM_NAME), "sales-post").build()
                    .login().user(bburkeUser).build()
                    .processSamlResponse(SamlClient.Binding.POST)
                    .transformObject(r -> { return checkSessionNotOnOrAfter(r, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE); })
                    .build()
                    .execute();
        }
    }

    @Test
    public void testSamlResponseContainsSessionNotOnOrAfterAuthnLogin() throws Exception {
        try(AutoCloseable c = new RealmAttributeUpdater(adminClient.realm(REALM_NAME))
                .updateWith(r -> {
                    r.setSsoSessionMaxLifespan(SSO_MAX_LIFESPAN);
                    r.setAccessCodeLifespan(ACCESS_CODE_LIFESPAN);
                    r.setAccessTokenLifespan(ACCESS_TOKEN_LIFESPAN);
                })
                .update()) {
            new SamlClientBuilder()
                    .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.POST)
                        .build()
                    .login().user(bburkeUser).build()
                    .processSamlResponse(SamlClient.Binding.POST)
                        .transformObject(r -> { return checkSessionNotOnOrAfter(r, SSO_MAX_LIFESPAN, ACCESS_CODE_LIFESPAN, ACCESS_TOKEN_LIFESPAN); })
                        .build()
                    .execute();
        }
    }

    @Test
    public void testSamlResponseClientConfigurationIdpInitiatedLogin() throws Exception {
        int ssoMaxLifespan = adminClient.realm(REALM_NAME).toRepresentation().getSsoSessionMaxLifespan();
        try(AutoCloseable c = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
                .setAttribute(SamlConfigAttributes.SAML_ASSERTION_LIFESPAN, "2000")
                .update()) {
             new SamlClientBuilder()
                    .idpInitiatedLogin(getAuthServerSamlEndpoint(REALM_NAME), "sales-post").build()
                    .login().user(bburkeUser).build()
                    .processSamlResponse(SamlClient.Binding.POST)
                        .transformObject(r -> { return checkSessionNotOnOrAfter(r, ssoMaxLifespan, 2000, 2000); })
                        .build()
                    .execute();
        }
    }

    @Test
    public void testSamlResponseClientConfigurationAfterAuthnLogin() throws Exception {
        int ssoMaxLifespan = adminClient.realm(REALM_NAME).toRepresentation().getSsoSessionMaxLifespan();
        try(AutoCloseable c = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
                .setAttribute(SamlConfigAttributes.SAML_ASSERTION_LIFESPAN, "1800")
                .update()) {
            new SamlClientBuilder()
                    .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.POST)
                        .build()
                    .login().user(bburkeUser).build()
                    .processSamlResponse(SamlClient.Binding.POST)
                        .transformObject(r -> { return checkSessionNotOnOrAfter(r, ssoMaxLifespan, 1800, 1800); })
                        .build()
                    .execute();
        }
    }
}
