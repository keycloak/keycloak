package org.keycloak.testsuite.console.clients;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.console.page.clients.credentials.SAMLClientCredentialsForm;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.auth.page.login.Login.SAML;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>
 */
public class ClientSAMLKeysTest extends AbstractClientTest {

    private ClientRepresentation newClient;

    @Page
    private SAMLClientCredentialsForm samlForm;

    @Before
    public void before() {
        newClient = createClientRep("client-saml", SAML);
        createClient(newClient);
    }

    @Test
    public void importSAMLKeyPEM() {
        samlForm.importPemCertificateKey();
        assertEquals("Expected key upload", "Success! Keystore uploaded successfully.", samlForm.getSuccessMessage());
    }

    @Test
    public void importSAMLKeyJKS() {
        samlForm.importJKSKey();
        assertEquals("Expected key upload", "Success! Keystore uploaded successfully.", samlForm.getSuccessMessage());
    }

    @Test
    public void importSAMLKeyPKCS12() {
        samlForm.importPKCS12Key();
        assertEquals("Expected key upload", "Success! Keystore uploaded successfully.", samlForm.getSuccessMessage());
    }
}
