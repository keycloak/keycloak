package org.keycloak.testsuite.forms;

import static org.keycloak.userprofile.DeclarativeUserProfileProvider.REALM_USER_PROFILE_ENABLED;

import java.util.HashMap;

import org.keycloak.representations.idm.RealmRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UserProfileRegisterTest extends RegisterTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        super.configureTestRealm(testRealm);

        if (testRealm.getAttributes() == null) {
            testRealm.setAttributes(new HashMap<>());
        }

        testRealm.getAttributes().put(REALM_USER_PROFILE_ENABLED, Boolean.TRUE.toString());
    }
}
