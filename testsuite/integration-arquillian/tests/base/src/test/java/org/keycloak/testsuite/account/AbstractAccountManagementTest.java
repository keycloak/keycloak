package org.keycloak.testsuite.account;

import java.util.List;
import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.page.auth.AuthRealm;
import static org.keycloak.testsuite.page.auth.AuthRealm.TEST;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractAccountManagementTest extends AbstractKeycloakTest {
    
    @Page
    protected AuthRealm authRealm;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        authRealm.setAuthRealm(TEST);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = new RealmRepresentation();
        testRealmRep.setRealm(TEST);
        testRealmRep.setEnabled(true);
        testRealms.add(testRealmRep);
    }

}
