package org.keycloak.testsuite.admin;

import static org.keycloak.testsuite.forms.VerifyProfileTest.PERMISSIONS_ALL;
import static org.keycloak.testsuite.forms.VerifyProfileTest.enableDynamicUserProfile;
import static org.keycloak.testsuite.forms.VerifyProfileTest.setUserProfileConfiguration;

import org.junit.Before;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.forms.VerifyProfileTest;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DeclarativeUserTest extends UserTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        super.configureTestRealm(testRealm);
        enableDynamicUserProfile(testRealm);
    }

    @Before
    public void onBefore() {
        setUserProfileConfiguration(testRealm(), "{\"attributes\": [{\"name\": \"aName\", " + PERMISSIONS_ALL + "}]}");
    }
}
