package org.keycloak.testsuite.console.clients;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.util.ArtifactBindingUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.clients.Client;
import org.keycloak.testsuite.console.page.clients.Clients;
import org.keycloak.testsuite.console.page.clients.CreateClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.auth.page.login.OIDCLogin.OIDC;
import static org.keycloak.testsuite.auth.page.login.OIDCLogin.SAML;
import static org.keycloak.testsuite.console.page.clients.settings.ClientSettingsForm.SAMLClientSettingsForm.SAML_AUTHNSTATEMENT;
import static org.keycloak.testsuite.console.page.clients.settings.ClientSettingsForm.SAMLClientSettingsForm.SAML_CLIENT_SIGNATURE;
import static org.keycloak.testsuite.console.page.clients.settings.ClientSettingsForm.SAMLClientSettingsForm.SAML_FORCE_NAME_ID_FORMAT;
import static org.keycloak.testsuite.console.page.clients.settings.ClientSettingsForm.SAMLClientSettingsForm.SAML_FORCE_POST_BINDING;
import static org.keycloak.testsuite.console.page.clients.settings.ClientSettingsForm.SAMLClientSettingsForm.SAML_NAME_ID_FORMAT;
import static org.keycloak.testsuite.console.page.clients.settings.ClientSettingsForm.SAMLClientSettingsForm.SAML_ONETIMEUSE_CONDITION;
import static org.keycloak.testsuite.console.page.clients.settings.ClientSettingsForm.SAMLClientSettingsForm.SAML_SERVER_SIGNATURE;
import static org.keycloak.testsuite.console.page.clients.settings.ClientSettingsForm.SAMLClientSettingsForm.SAML_SIGNATURE_ALGORITHM;
import static org.keycloak.testsuite.util.AttributesAssert.assertEqualsBooleanAttributes;
import static org.keycloak.testsuite.util.AttributesAssert.assertEqualsListAttributes;
import static org.keycloak.testsuite.util.AttributesAssert.assertEqualsStringAttributes;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractClientTest extends AbstractConsoleTest {

    public final String TEST_CLIENT_ID = "test-client";
    public final List<String> TEST_REDIRECT_URIs = Arrays.asList(new String[] { "http://example.test/app/" });

    @Page
    protected Clients clientsPage;
    @Page
    protected Client clientPage; // note: cannot call navigateTo() unless client id is set
    @Page
    protected CreateClient createClientPage;

    @Before
    public void beforeClientTest() {
        clientsPage.navigateTo();
    }

    public void createClient(ClientRepresentation client) {
        assertCurrentUrlEquals(clientsPage);
        clientsPage.table().createClient();
        createClientPage.form().setValues(client);
        createClientPage.form().save();
        assertAlertSuccess();
    }

    public static ClientRepresentation createClientRep(String clientId, String protocol) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        client.setEnabled(true);
        client.setProtocol(protocol);

        client.setDirectAccessGrantsEnabled(true);
        client.setFullScopeAllowed(true);
        client.setPublicClient(true);
        client.setStandardFlowEnabled(true);

        if (protocol.equals(SAML)) {
            client.setAttributes(getSAMLAttributes());
        }
        return client;
    }

    public static Map<String, String> getSAMLAttributes() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(SAML_CLIENT_SIGNATURE, "true");
        attributes.put(SAML_AUTHNSTATEMENT, "true");
        attributes.put(SAML_FORCE_POST_BINDING, "true");
        attributes.put(SAML_SERVER_SIGNATURE, "true");
        attributes.put(SAML_SIGNATURE_ALGORITHM, "RSA_SHA256");
        attributes.put(SAML_FORCE_NAME_ID_FORMAT, "false");
        attributes.put(SAML_NAME_ID_FORMAT, "username");
        attributes.put(SamlConfigAttributes.SAML_ARTIFACT_BINDING_IDENTIFIER, ArtifactBindingUtils.computeArtifactBindingIdentifierString("saml"));
        return attributes;
    }

    public ClientRepresentation findClientByClientId(String clientId) {
        ClientRepresentation found = null;
        for (ClientRepresentation clientRepresentation : testRealmResource().clients().findAll()) {
            if (clientRepresentation.getClientId().equals(clientId)) {
                found = clientRepresentation;
                break;
            }
        }
        return found;
    }

    public void assertClientSettingsEqual(ClientRepresentation c1, ClientRepresentation c2) {
        assertEqualsStringAttributes(c1.getClientId(), c2.getClientId());
        assertEqualsStringAttributes(c1.getName(), c2.getName());
        assertEqualsBooleanAttributes(c1.isEnabled(), c2.isEnabled());
        assertEqualsBooleanAttributes(c1.isAlwaysDisplayInConsole(), c2.isAlwaysDisplayInConsole());
        assertEqualsStringAttributes(c1.getBaseUrl(), c2.getBaseUrl());
        assertEqualsBooleanAttributes(c1.isConsentRequired(), c2.isConsentRequired());
        assertEqualsStringAttributes(c1.getProtocol(), c2.getProtocol());
        assertEqualsListAttributes(c1.getRedirectUris(), c2.getRedirectUris());

        if (c1.getProtocol().equals(OIDC)) {
            assertEqualsBooleanAttributes(c1.isBearerOnly(), c2.isBearerOnly());
            assertEqualsBooleanAttributes(c1.isDirectAccessGrantsEnabled(), c2.isDirectAccessGrantsEnabled());
            assertEqualsBooleanAttributes(c1.isPublicClient(), c2.isPublicClient());
            assertEqualsListAttributes(c1.getWebOrigins(), c2.getWebOrigins());
            assertEqualsStringAttributes(c1.getAdminUrl(), c2.getAdminUrl());
            assertEqualsBooleanAttributes(c1.isSurrogateAuthRequired(), c2.isSurrogateAuthRequired());
            assertEqualsBooleanAttributes(c1.isServiceAccountsEnabled(), c2.isServiceAccountsEnabled());
        }
    }

    public void assertClientSamlAttributes(Map<String, String> expected, Map<String, String> actual) {
        for (String key : expected.keySet()) {
            assertEquals("Expected attribute " + key, expected.get(key), actual.get(key));
        }
    }

    public ProtocolMapperRepresentation findClientMapperByName(String clientId, String mapperName) {
        ProtocolMapperRepresentation found = null;
        for (ProtocolMapperRepresentation mapper : testRealmResource().clients().get(clientId).getProtocolMappers()
                .getMappers()) {
            if (mapperName.equals(mapper.getName())) {
                found = mapper;
            }
        }
        return found;
    }

    public ClientsResource clientsResource() {
        return testRealmResource().clients();
    }

    public ClientResource clientResource(String id) {
        return clientsResource().get(id);
    }

}
