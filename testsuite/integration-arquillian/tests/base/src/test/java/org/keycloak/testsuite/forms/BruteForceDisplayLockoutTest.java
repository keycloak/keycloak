package org.keycloak.testsuite.forms;

import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;

public class BruteForceDisplayLockoutTest extends BruteForceTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.setDisplayLockoutOnLogin(true);
        super.configureTestRealm(testRealm);
    }

    @Override
    public void expectTemporarilyDisabled(String username, String userId, String password) throws Exception {
        loginPage.open();
        loginPage.login(username, password);

        loginPage.assertCurrent();
        String src = driver.getPageSource();
        Assert.assertEquals("Account is temporarily disabled, contact admin or try again later.", loginPage.getError());
        AssertEvents.ExpectedEvent event = events.expectLogin()
                .session((String) null)
                .error(Errors.USER_TEMPORARILY_DISABLED)
                .detail(Details.USERNAME, username)
                .removeDetail(Details.CONSENT);
        if (userId != null) {
            event.user(userId);
        }
        event.assertEvent();
    }

}
