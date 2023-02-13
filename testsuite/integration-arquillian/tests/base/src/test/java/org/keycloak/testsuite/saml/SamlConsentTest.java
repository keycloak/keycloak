package org.keycloak.testsuite.saml;

import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.junit.Test;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.utils.io.IOUtil;

import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

/**
 * @author mhajas
 */
public class SamlConsentTest extends AbstractSamlTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(IOUtil.loadRealm("/adapter-test/keycloak-saml/testsaml.json"));
    }

    @Test
    public void rejectedConsentResponseTest() throws ParsingException, ConfigurationException, ProcessingException {
        ClientRepresentation client = adminClient.realm(REALM_NAME)
                .clients()
                .findByClientId(SAML_CLIENT_ID_SALES_POST)
                .get(0);

        adminClient.realm(REALM_NAME)
                .clients()
                .get(client.getId())
                .update(ClientBuilder.edit(client)
                        .consentRequired(true)
                        .attribute(SamlProtocol.SAML_IDP_INITIATED_SSO_URL_NAME, "sales-post")
                        .attribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, SAML_ASSERTION_CONSUMER_URL_SALES_POST + "saml")
                        .attribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true")
                        .build());

        log.debug("Log in using idp initiated login");
        SAMLDocumentHolder documentHolder = new SamlClientBuilder()
          .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, Binding.POST).build()
          .login().user(bburkeUser).build()
          .consentRequired().approveConsent(false).build()
          .getSamlResponse(Binding.POST);

        final String samlDocumentString = IOUtil.documentToString(documentHolder.getSamlDocument());
        assertThat(samlDocumentString, containsString("<dsig:Signature")); // KEYCLOAK-4262
        assertThat(samlDocumentString, not(containsString("<samlp:LogoutResponse"))); // KEYCLOAK-4261
        assertThat(samlDocumentString, containsString("<samlp:Response")); // KEYCLOAK-4261
        assertThat(samlDocumentString, containsString("<samlp:Status")); // KEYCLOAK-4181
        assertThat(samlDocumentString, containsString("<samlp:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:RequestDenied\"")); // KEYCLOAK-4181
    }
}
