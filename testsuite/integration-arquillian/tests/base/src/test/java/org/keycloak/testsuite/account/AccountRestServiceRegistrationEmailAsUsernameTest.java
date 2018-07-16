package org.keycloak.testsuite.account;

import org.keycloak.representations.idm.RealmRepresentation;

/**
 * @author Moritz Becker (moritz.becker@ordami.com)
 * @date 16/07/2018
 * @company ordami GmbH
 */
public class AccountRestServiceRegistrationEmailAsUsernameTest extends AccountRestServiceTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        super.configureTestRealm(testRealm);
        testRealm.setRegistrationEmailAsUsername(true);
    }
}
