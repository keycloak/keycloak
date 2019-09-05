package org.keycloak.testsuite.saml;

import org.junit.Test;
import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.StatementAbstractType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;

import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author mhajas
 */
public class SessionNotOnOrAfterTest extends AbstractSamlTest {

    private static final Integer SSO_MAX_LIFESPAN = 3602;

    private SAML2Object checkSessionNotOnOrAfter(SAML2Object ob) {
        assertThat(ob, Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        ResponseType resp = (ResponseType) ob;
        Set<StatementAbstractType> statements = resp.getAssertions().get(0).getAssertion().getStatements();

        AuthnStatementType authType = statements.stream()
                .filter(statement -> statement instanceof AuthnStatementType)
                .map(s -> (AuthnStatementType) s)
                .findFirst().orElse(null);

        assertThat(authType, notNullValue());
        assertThat(authType.getSessionNotOnOrAfter(), notNullValue());
        assertThat(authType.getSessionNotOnOrAfter(), is(XMLTimeUtil.add(authType.getAuthnInstant(), SSO_MAX_LIFESPAN * 1000)));

        return null;
    }

    @Test
    public void testSamlResponseContainsSessionNotOnOrAfterIdpInitiatedLogin() throws Exception {
        try(AutoCloseable c = new RealmAttributeUpdater(adminClient.realm(REALM_NAME))
            .updateWith(r -> r.setSsoSessionMaxLifespan(SSO_MAX_LIFESPAN))
            .update()) {
             new SamlClientBuilder()
                    .idpInitiatedLogin(getAuthServerSamlEndpoint(REALM_NAME), "sales-post").build()
                    .login().user(bburkeUser).build()
                    .processSamlResponse(SamlClient.Binding.POST)
                        .transformObject(this::checkSessionNotOnOrAfter)
                        .build()
                    .execute();
        }
    }

    @Test
    public void testSamlResponseContainsSessionNotOnOrAfterAuthnLogin() throws Exception {
        try(AutoCloseable c = new RealmAttributeUpdater(adminClient.realm(REALM_NAME))
                .updateWith(r -> r.setSsoSessionMaxLifespan(SSO_MAX_LIFESPAN))
                .update()) {
            new SamlClientBuilder()
                    .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.POST)
                        .build()
                    .login().user(bburkeUser).build()
                    .processSamlResponse(SamlClient.Binding.POST)
                        .transformObject(this::checkSessionNotOnOrAfter)
                        .build()
                    .execute();
        }
    }
}
