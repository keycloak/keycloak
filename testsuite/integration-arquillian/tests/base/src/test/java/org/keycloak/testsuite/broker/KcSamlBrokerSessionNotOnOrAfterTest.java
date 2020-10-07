package org.keycloak.testsuite.broker;

import org.junit.Test;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.StatementAbstractType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;

import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_SAML_ALIAS;
import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_CONS_NAME;
import static org.keycloak.testsuite.broker.BrokerTestConstants.USER_EMAIL;
import static org.keycloak.testsuite.broker.BrokerTestConstants.USER_LOGIN;
import static org.keycloak.testsuite.broker.BrokerTestConstants.USER_PASSWORD;

public class KcSamlBrokerSessionNotOnOrAfterTest extends AbstractBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcSamlBrokerConfiguration.INSTANCE;
    }

    @Test
    public void testConsumerIdpInitiatedLoginContainsSessionNotOnOrAfter() throws Exception {
        SAMLDocumentHolder samlResponse = new SamlClientBuilder()
                .idpInitiatedLogin(getConsumerSamlEndpoint(REALM_CONS_NAME), "sales-post").build()
                // Request login via kc-saml-idp
                .login().idp(IDP_SAML_ALIAS).build()

                .processSamlResponse(SamlClient.Binding.POST)    // AuthnRequest to producer IdP
                    .targetAttributeSamlRequest()
                    .build()

                // Login in provider realm
                .login().user(USER_LOGIN, USER_PASSWORD).build()

                // Send the response to the consumer realm
                .processSamlResponse(SamlClient.Binding.POST).build()

                // Create account in comsumer realm
                .updateProfile().username(USER_LOGIN).email(USER_EMAIL).firstName("Firstname").lastName("Lastname").build()
                .followOneRedirect()

                // Obtain the response sent to the app
                .getSamlResponse(SamlClient.Binding.POST);

        assertThat(samlResponse.getSamlObject(), Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        ResponseType resp = (ResponseType) samlResponse.getSamlObject();
        Set<StatementAbstractType> statements = resp.getAssertions().get(0).getAssertion().getStatements();

        AuthnStatementType authType = statements.stream()
                .filter(statement -> statement instanceof AuthnStatementType)
                .map(s -> (AuthnStatementType) s)
                .findFirst().orElse(null);

        assertThat(authType, notNullValue());
        assertThat(authType.getSessionNotOnOrAfter(), notNullValue());
        assertThat(authType.getSessionNotOnOrAfter(), is(XMLTimeUtil.add(authType.getAuthnInstant(), adminClient.realm(REALM_CONS_NAME).toRepresentation().getSsoSessionMaxLifespan() * 1000)));
    }
}
