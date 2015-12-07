package org.keycloak.testsuite.account;

import org.jboss.arquillian.graphene.page.Page;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.keycloak.testsuite.AbstractAuthTest;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import org.keycloak.testsuite.auth.page.account.AccountManagement;
import org.keycloak.testsuite.auth.page.account.fragment.AccountManagementAlert;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractAccountManagementTest extends AbstractAuthTest {

    @Page
    protected AccountManagement testRealmAccountManagementPage;

    @FindBy(className = "alert")
    protected AccountManagementAlert alert;

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

    public void assertAlertSuccess() {
        alert.waitUntilPresentAndClassSet();
        assertTrue(alert.isSuccess());
    }

    public void assertAlertError() {
        alert.waitUntilPresentAndClassSet();
        assertTrue(alert.isError());
    }

}
