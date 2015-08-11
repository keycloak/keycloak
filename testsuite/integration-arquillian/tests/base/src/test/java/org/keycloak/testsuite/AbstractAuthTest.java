package org.keycloak.testsuite;

import java.text.MessageFormat;
import java.util.List;
import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.Page;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.keycloak.admin.client.resource.RealmResource;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;
import static org.keycloak.testsuite.admin.ApiUtil.resetUserPassword;
import static org.keycloak.testsuite.admin.Users.setPasswordFor;
import org.keycloak.testsuite.auth.page.AuthRealm;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.console.page.fragment.FlashMessage;
import org.openqa.selenium.Cookie;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractAuthTest extends AbstractKeycloakTest {

    @Page
    protected AuthRealm testRealm;
    @Page
    protected OIDCLogin testRealmLogin;

    protected UserRepresentation testRealmUser;

    @FindByJQuery(".alert")
    protected FlashMessage flashMessage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = new RealmRepresentation();
        testRealmRep.setRealm(TEST);
        testRealmRep.setEnabled(true);
        testRealms.add(testRealmRep);
    }

    @Before
    public void beforeAuthTest() {
        testRealmLogin.setAuthRealm(testRealm);

        testRealmUser = createUserRepresentation("test", "test@email.test", "test", "user", true);
        setPasswordFor(testRealmUser, PASSWORD);

        deleteAllCookiesForTestRealm();
    }

    public void createTestUserWithAdminClient() {
        String id = createUserWithAdminClient(testRealmResource(), testRealmUser);
        testRealmUser.setId(id);
        resetUserPassword(testRealmResource().users().get(id), PASSWORD, false);
    }

    public static UserRepresentation createUserRepresentation(String username, String email, String firstName, String lastName, boolean enabled) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(enabled);
        return user;
    }

    public void deleteAllCookiesForTestRealm() {
        testRealm.navigateTo();
        driver.manage().deleteAllCookies();
    }

    public void listCookies() {
        System.out.println("LIST OF COOKIES: ");
        for (Cookie c : driver.manage().getCookies()) {
            System.out.println(MessageFormat.format(" {1} {2} {0}",
                    c.getName(), c.getDomain(), c.getPath(), c.getValue()));
        }
    }

    public void assertFlashMessageSuccess() {
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
    }

    public void assertFlashMessageDanger() {
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());
    }

    public void assertFlashMessageError() {
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isError());
    }

    private void logCurrentUrl() {
        System.out.println("Current URL: " + driver.getCurrentUrl());
    }

    public RealmResource testRealmResource() {
        return adminClient.realm(testRealm.getAuthRealm());
    }

}
