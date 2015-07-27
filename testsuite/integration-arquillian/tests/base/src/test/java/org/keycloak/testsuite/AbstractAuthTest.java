package org.keycloak.testsuite;

import java.util.List;
import org.keycloak.representations.idm.RealmRepresentation;
import static org.keycloak.testsuite.console.page.Realm.TEST;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractAuthTest extends AbstractKeycloakTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = new RealmRepresentation();
        testRealmRep.setRealm(TEST);
        testRealmRep.setEnabled(true);
        testRealms.add(testRealmRep);
    }

}
