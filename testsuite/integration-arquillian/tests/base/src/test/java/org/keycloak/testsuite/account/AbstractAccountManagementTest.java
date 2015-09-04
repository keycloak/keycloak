package org.keycloak.testsuite.account;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.keycloak.testsuite.AbstractAuthTest;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import org.keycloak.testsuite.auth.page.account.AccountManagement;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractAccountManagementTest extends AbstractAuthTest {

    @Page
    protected AccountManagement testRealmAccountManagementPage;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(TEST);
        testRealmAccountManagementPage.setAuthRealm(TEST);
    }

    @Before
    public void beforeAbstractAccountTest() {
        // make user test user exists in test realm
        createTestUserWithAdminClient();
    }
    
}
