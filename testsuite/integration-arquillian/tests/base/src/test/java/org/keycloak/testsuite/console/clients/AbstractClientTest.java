package org.keycloak.testsuite.console.clients;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.arquillian.graphene.page.Page;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import static org.keycloak.testsuite.auth.page.login.OIDCLogin.OIDC;
import static org.keycloak.testsuite.auth.page.login.OIDCLogin.SAML;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.clients.Client;
import org.keycloak.testsuite.console.page.clients.Clients;
import org.keycloak.testsuite.console.page.clients.CreateClient;
import org.keycloak.testsuite.console.page.clients.CreateClientForm.OidcAccessType;
import static org.keycloak.testsuite.console.page.clients.CreateClientForm.OidcAccessType.*;
import static org.keycloak.testsuite.console.page.clients.CreateClientForm.SAMLClientSettingsForm.SAML_ASSERTION_CONSUMER_URL_POST;
import static org.keycloak.testsuite.console.page.clients.CreateClientForm.SAMLClientSettingsForm.SAML_ASSERTION_CONSUMER_URL_REDIRECT;
import static org.keycloak.testsuite.console.page.clients.CreateClientForm.SAMLClientSettingsForm.SAML_ASSERTION_SIGNATURE;
import static org.keycloak.testsuite.console.page.clients.CreateClientForm.SAMLClientSettingsForm.SAML_AUTHNSTATEMENT;
import static org.keycloak.testsuite.console.page.clients.CreateClientForm.SAMLClientSettingsForm.SAML_CLIENT_SIGNATURE;
import static org.keycloak.testsuite.console.page.clients.CreateClientForm.SAMLClientSettingsForm.SAML_ENCRYPT;
import static org.keycloak.testsuite.console.page.clients.CreateClientForm.SAMLClientSettingsForm.SAML_FORCE_NAME_ID_FORMAT;
import static org.keycloak.testsuite.console.page.clients.CreateClientForm.SAMLClientSettingsForm.SAML_FORCE_POST_BINDING;
import static org.keycloak.testsuite.console.page.clients.CreateClientForm.SAMLClientSettingsForm.SAML_MULTIVALUED_ROLES;
import static org.keycloak.testsuite.console.page.clients.CreateClientForm.SAMLClientSettingsForm.SAML_NAME_ID_FORMAT;
import static org.keycloak.testsuite.console.page.clients.CreateClientForm.SAMLClientSettingsForm.SAML_SERVER_SIGNATURE;
import static org.keycloak.testsuite.console.page.clients.CreateClientForm.SAMLClientSettingsForm.SAML_SIGNATURE_ALGORITHM;
import static org.keycloak.testsuite.console.page.clients.CreateClientForm.SAMLClientSettingsForm.SAML_SIGNATURE_CANONICALIZATION_METHOD;
import static org.keycloak.testsuite.console.page.clients.CreateClientForm.SAMLClientSettingsForm.SAML_SINGLE_LOGOUT_SERVICE_URL_POST;
import static org.keycloak.testsuite.console.page.clients.CreateClientForm.SAMLClientSettingsForm.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT;
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
    public final String TEST_REDIRECT_URIS = "http://example.test/app/*";
    
    @Page
    protected Clients clientsPage;
    @Page
    protected Client clientPage; // note: cannot call navigateTo() unless client id is set
    @Page
    protected CreateClient createClientPage;

    @Before
    public void beforeClientTest() {
//        configure().clients();
        clientsPage.navigateTo();
    }

    public void createClient(ClientRepresentation client) {
        assertCurrentUrlEquals(clientsPage);
        clientsPage.table().createClient();
        createClientPage.form().setValues(client);
        if (SAML.equals(client.getProtocol())) {
            createClientPage.form().samlForm().setValues(client);
        }
        createClientPage.form().save();
    }

    private static ClientRepresentation createClientRep(String clientId) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        client.setEnabled(true);
        client.setConsentRequired(false);
        return client;
    }
    
    public static ClientRepresentation createOidcClientRep(OidcAccessType accessType, String clientId, String... redirectUris) {
        ClientRepresentation client = createClientRep(clientId);
       
        client.setProtocol(OIDC);
        
        switch (accessType) {
            case BEARER_ONLY:
                client.setBearerOnly(true);
                break;
            case PUBLIC:
                client.setBearerOnly(false);
                client.setPublicClient(true);
                client.setStandardFlowEnabled(true);
                client.setImplicitFlowEnabled(false);
                client.setDirectAccessGrantsEnabled(true);
                setRedirectUris(client, redirectUris);
                break;
            case CONFIDENTIAL:
                client.setBearerOnly(false);
                client.setPublicClient(false);
                client.setStandardFlowEnabled(true);
                client.setDirectAccessGrantsEnabled(true);
                client.setServiceAccountsEnabled(true);
                setRedirectUris(client, redirectUris);
                break;
        }
        return client;
    }
    
    public static ClientRepresentation createSamlClientRep(String clinetId) {
        ClientRepresentation client = createClientRep(clinetId);
        
        client.setProtocol(SAML);
        
        client.setFrontchannelLogout(true);
        client.setAttributes(getSAMLAttributes());
        
        return client;
    }
    
    private static void setRedirectUris(ClientRepresentation client, String... redirectUris) {
        List<String> redirectUrisList = new ArrayList();
        redirectUrisList.addAll(Arrays.asList(redirectUris));
        client.setRedirectUris(redirectUrisList);
    }
    
    protected static void setExpectedWebOrigins(ClientRepresentation client) {
        List<String> webOrigins = new ArrayList<>();
        for (String redirectUri : client.getRedirectUris()) {
            //parse webOrigin from redirectUri: take substring from index 0 to 
            //first occurence of "/", excluded "http://" by starting search on index 7
            webOrigins.add(redirectUri.substring(0, redirectUri.indexOf("/", 7)));
        }
        client.setWebOrigins(webOrigins);
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
        assertEqualsBooleanAttributes(c1.isConsentRequired(), c2.isConsentRequired());
        assertEqualsBooleanAttributes(c1.isDirectAccessGrantsEnabled(), c2.isDirectAccessGrantsEnabled());
        assertEqualsStringAttributes(c1.getProtocol(), c2.getProtocol());

        assertEqualsBooleanAttributes(c1.isBearerOnly(), c2.isBearerOnly());
        assertEqualsBooleanAttributes(c1.isPublicClient(), c2.isPublicClient());
        assertEqualsBooleanAttributes(c1.isSurrogateAuthRequired(), c2.isSurrogateAuthRequired());

        assertEqualsBooleanAttributes(c1.isFrontchannelLogout(), c2.isFrontchannelLogout());

        assertEqualsBooleanAttributes(c1.isServiceAccountsEnabled(), c2.isServiceAccountsEnabled());
        assertEqualsListAttributes(c1.getRedirectUris(), c2.getRedirectUris());
        assertEqualsStringAttributes(c1.getBaseUrl(), c2.getBaseUrl());
        assertEqualsStringAttributes(c1.getAdminUrl(), c2.getAdminUrl());
        assertEqualsListAttributes(c1.getWebOrigins(), c2.getWebOrigins());
    }
    
    public void assertClientSamlAttributes(Map<String, String> expected, Map<String, String> actual) {
        for (String key : expected.keySet()) {
            assertEquals("Expected attribute " + key, expected.get(key), actual.get(key));
        }
    }
    
    protected static Map<String, String> getSAMLAttributes() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(SAML_ASSERTION_SIGNATURE, "true");
        attributes.put(SAML_AUTHNSTATEMENT, "false");
	attributes.put(SAML_CLIENT_SIGNATURE,	"true");
	attributes.put(SAML_ENCRYPT, "true");
	attributes.put(SAML_FORCE_POST_BINDING, "true");
	attributes.put(SAML_MULTIVALUED_ROLES, "false");
	attributes.put(SAML_SERVER_SIGNATURE,	"true");
	attributes.put(SAML_SIGNATURE_ALGORITHM, "RSA_SHA512");
	attributes.put(SAML_ASSERTION_CONSUMER_URL_POST, "http://example0.test");
	attributes.put(SAML_ASSERTION_CONSUMER_URL_REDIRECT, "http://example1.test");
	attributes.put(SAML_FORCE_NAME_ID_FORMAT, "true");
	attributes.put(SAML_NAME_ID_FORMAT, "email");
	attributes.put(SAML_SIGNATURE_CANONICALIZATION_METHOD, "http://www.w3.org/2001/10/xml-exc-c14n#WithComments");
	attributes.put(SAML_SINGLE_LOGOUT_SERVICE_URL_POST, "http://example2.test");
	attributes.put(SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT, "http://example3.test");
        return attributes;
    }
    
    public ProtocolMapperRepresentation findClientMapperByName(String clientId, String mapperName) {
        ProtocolMapperRepresentation found = null;
        for (ProtocolMapperRepresentation mapper : testRealmResource().clients().get(clientId).getProtocolMappers().getMappers()) {
            if (mapperName.equals(mapper.getName())) {
                found = mapper;
            }
        }
        return found;
    }
}
