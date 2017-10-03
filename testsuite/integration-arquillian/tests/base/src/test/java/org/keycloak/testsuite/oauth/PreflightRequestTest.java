package org.keycloak.testsuite.oauth;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class PreflightRequestTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = new RealmRepresentation();
        testRealmRep.setId(TEST);
        testRealmRep.setRealm(TEST);
        testRealmRep.setEnabled(true);
        testRealms.add(testRealmRep);
    }

    @Test
    public void preflightRequest() throws Exception {
        CloseableHttpResponse response = oauth.doPreflightRequest();

        String[] methods = response.getHeaders("Access-Control-Allow-Methods")[0].getValue().split(", ");
        Set allowedMethods = new HashSet(Arrays.asList(methods));

        assertEquals(2, allowedMethods.size());
        assertTrue(allowedMethods.containsAll(Arrays.asList("POST", "OPTIONS")));
    }
}
