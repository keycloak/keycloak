package org.keycloak.testsuite.keys;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.KeyType;
import org.keycloak.keys.GeneratedEcdsaKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation.KeyMetadataRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;

// KEYCLOAK-7560 Refactoring Token Signing and Verifying by Token Signature SPI

public class GeneratedEcdsaKeyProviderTest extends AbstractKeycloakTest {
    private static final String DEFAULT_EC = "P-256";
    private static final String ECDSA_ELLIPTIC_CURVE_KEY = "ecdsaEllipticCurveKey";
    private static final String TEST_REALM_NAME = "test";

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }

    @Test
    public void defaultEc() throws Exception {
        supportedEc(null);
    }

    @Test
    public void supportedEcP521() throws Exception {
        supportedEc("P-521");
    }

    @Test
    public void supportedEcP384() throws Exception {
        supportedEc("P-384");
    }

    @Test
    public void supportedEcP256() throws Exception {
        supportedEc("P-256");
    }

    @Test
    public void unsupportedEcK163() throws Exception {
        // NIST.FIPS.186-4 Koblitz Curve over Binary Field
        unsupportedEc("K-163");
    }
    
    private void supportedEc(String ecInNistRep) {
        long priority = System.currentTimeMillis();

        ComponentRepresentation rep = createRep("valid", GeneratedEcdsaKeyProviderFactory.ID);
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle("priority", Long.toString(priority));
        if (ecInNistRep != null) {
            rep.getConfig().putSingle(ECDSA_ELLIPTIC_CURVE_KEY, ecInNistRep);
        } else {
            ecInNistRep = DEFAULT_EC;
        }

        Response response = adminClient.realm(TEST_REALM_NAME).components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        getCleanup().addComponentId(id);
        response.close();

        ComponentRepresentation createdRep = adminClient.realm(TEST_REALM_NAME).components().component(id).toRepresentation();

        // stands for the number of properties in the key provider config
        assertEquals(2, createdRep.getConfig().size());
        assertEquals(Long.toString(priority), createdRep.getConfig().getFirst("priority"));
        assertEquals(ecInNistRep, createdRep.getConfig().getFirst(ECDSA_ELLIPTIC_CURVE_KEY));

        KeysMetadataRepresentation keys = adminClient.realm(TEST_REALM_NAME).keys().getKeyMetadata();

        KeysMetadataRepresentation.KeyMetadataRepresentation key = null;

        for (KeyMetadataRepresentation k : keys.getKeys()) {
           if (KeyType.EC.equals(k.getType()) && id.equals(k.getProviderId())) {
                key = k;
                break;
           }
        }
        assertNotNull(key);

        assertEquals(id, key.getProviderId());
        assertEquals(KeyType.EC, key.getType());
        assertEquals(priority, key.getProviderPriority());
    }

    private void unsupportedEc(String ecInNistRep) {
        long priority = System.currentTimeMillis();

        ComponentRepresentation rep = createRep("valid", GeneratedEcdsaKeyProviderFactory.ID);
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle("priority", Long.toString(priority));
        rep.getConfig().putSingle(ECDSA_ELLIPTIC_CURVE_KEY, ecInNistRep);
        boolean isEcAccepted = true;

        Response response = null;
        try {
            response = adminClient.realm(TEST_REALM_NAME).components().add(rep);
            String id = ApiUtil.getCreatedId(response);
            getCleanup().addComponentId(id);
            response.close();
        } catch (WebApplicationException e) {
            isEcAccepted = false;
        } finally {
            response.close();
        }
        assertEquals(isEcAccepted, false);
    }
    
    protected void assertErrror(Response response, String error) {
        if (!response.hasEntity()) {
            fail("No error message set");
        }

        ErrorRepresentation errorRepresentation = response.readEntity(ErrorRepresentation.class);
        assertEquals(error, errorRepresentation.getErrorMessage());
        response.close();
    }

    protected ComponentRepresentation createRep(String name, String providerId) {
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName(name);
        rep.setParentId(TEST_REALM_NAME);
        rep.setProviderId(providerId);
        rep.setProviderType(KeyProvider.class.getName());
        rep.setConfig(new MultivaluedHashMap<>());
        return rep;
    }
}
