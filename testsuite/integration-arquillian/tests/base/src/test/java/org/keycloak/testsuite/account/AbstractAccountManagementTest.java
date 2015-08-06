package org.keycloak.testsuite.account;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.AbstractAuthTest;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import org.keycloak.testsuite.auth.page.account.AccountManagement;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractAccountManagementTest extends AbstractAuthTest {

    @Page
    protected AccountManagement testRealmAccountManagement;
    
    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealm.setAuthRealm(TEST);
        testRealmAccountManagement.setAuthRealm(testRealm);
    }
    
}
