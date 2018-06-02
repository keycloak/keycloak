package org.keycloak.testsuite.keys;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jws.AlgorithmType;
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

public class GeneratedEcdsaKeyProviderTest extends AbstractKeycloakTest {
	// KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
	
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
        long priority = System.currentTimeMillis();

        ComponentRepresentation rep = createRep("valid", GeneratedEcdsaKeyProviderFactory.ID);
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle("priority", Long.toString(priority));

        Response response = adminClient.realm("test").components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        getCleanup().addComponentId(id);
        response.close();

        ComponentRepresentation createdRep = adminClient.realm("test").components().component(id).toRepresentation();
        MultivaluedHashMap<String, String> map = createdRep.getConfig();
        Set<String> set = map.keySet();
        for (String s : set) {
        	System.out.println("key = " + s + ", first value = " + map.getFirst(s));
        }

        assertEquals(2, createdRep.getConfig().size());
        assertEquals(Long.toString(priority), createdRep.getConfig().getFirst("priority"));
        assertEquals("P-256", createdRep.getConfig().getFirst("ecdsaEllipticCurveKey"));

        KeysMetadataRepresentation keys = adminClient.realm("test").keys().getKeyMetadata();

        KeysMetadataRepresentation.KeyMetadataRepresentation key = null;

        for (KeyMetadataRepresentation k : keys.getKeys()) {
           if (AlgorithmType.ECDSA.name().equals(k.getType()) && id.equals(k.getProviderId())) {
                key = k;
                break;
           }
        }
        assertNotNull(key);

        assertEquals(id, key.getProviderId());
        assertEquals(AlgorithmType.ECDSA.name(), key.getType());
        assertEquals(priority, key.getProviderPriority());

        JSONWebKeySet jwkSet = oauth.doCertsRequest("test");
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
        rep.setParentId("test");
        rep.setProviderId(providerId);
        rep.setProviderType(KeyProvider.class.getName());
        rep.setConfig(new MultivaluedHashMap<>());
        return rep;
    }
}
