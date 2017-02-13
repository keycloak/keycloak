package org.keycloak.testsuite.saml;

import org.junit.Test;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.IOUtil;
import org.keycloak.testsuite.util.SamlClient;

import java.net.URI;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.util.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.SamlClient.idpInitiatedLoginWithRequiredConsent;

/**
 * @author mhajas
 */
public class SamlConsentTest extends AbstractSamlTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(loadRealm("/adapter-test/keycloak-saml/testsaml.json"));
    }

    @Test
    public void rejectedConsentResponseTest() throws ParsingException, ConfigurationException, ProcessingException {
        ClientRepresentation client = adminClient.realm(REALM_NAME)
                .clients()
                .findByClientId(SAML_CLIENT_ID_SALES_POST_ENC)
                .get(0);

        adminClient.realm(REALM_NAME)
                .clients()
                .get(client.getId())
                .update(ClientBuilder.edit(client)
                        .consentRequired(true)
                        .attribute("saml.encrypt", "false") //remove after RHSSO-797
                        .attribute("saml_idp_initiated_sso_url_name", "sales-post-enc")
                        .attribute("saml_assertion_consumer_url_post", SAML_ASSERTION_CONSUMER_URL_SALES_POST_ENC + "saml")
                        .build());

        log.debug("Log in using idp initiated login");
        String idpInitiatedLogin = getAuthServerRoot() + "realms/" + REALM_NAME + "/protocol/saml/clients/sales-post-enc";
        SAMLDocumentHolder documentHolder = idpInitiatedLoginWithRequiredConsent(bburkeUser, URI.create(idpInitiatedLogin), SamlClient.Binding.POST, false);

        assertThat(IOUtil.documentToString(documentHolder.getSamlDocument()), containsString("<dsig:Signature")); // KEYCLOAK-4262
        assertThat(IOUtil.documentToString(documentHolder.getSamlDocument()), not(containsString("<samlp:LogoutResponse"))); // KEYCLOAK-4261
        assertThat(IOUtil.documentToString(documentHolder.getSamlDocument()), containsString("<samlp:Response")); // KEYCLOAK-4261
        assertThat(IOUtil.documentToString(documentHolder.getSamlDocument()), containsString("<samlp:Status")); // KEYCLOAK-4181
        assertThat(IOUtil.documentToString(documentHolder.getSamlDocument()), containsString("<samlp:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:RequestDenied\"")); // KEYCLOAK-4181
    }
}
