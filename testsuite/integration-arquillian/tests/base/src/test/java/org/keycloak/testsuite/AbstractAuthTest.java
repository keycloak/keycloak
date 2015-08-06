package org.keycloak.testsuite;

import java.util.List;
import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.Page;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.auth.page.AuthRealm;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.console.page.fragment.FlashMessage;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractAuthTest extends AbstractKeycloakTest {

    @Page
    protected AuthRealm testRealm;
    @Page
    protected OIDCLogin testRealmLogin;

    protected RealmResource testRealmResource;
//    protected UserRepresentation testRealmAdminUser;
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
        testRealmResource = keycloak.realm(testRealm.getAuthRealm());

        testRealmUser = createUserRepresentation("test", "test@email.test", "test", "user", true);

        deleteAllCookiesForTestRealm();
    }

//    private void createTestAdminWithAdminClient() {
//        testRealmAdminUser = createUserRepresentation("admin", "admin@email.test", "admin", "user", true);
//
//        createUser(testRealmResource, testRealmAdminUser);
//        testRealmAdminUser = findUserByUsername(testRealmResource, testRealmAdminUser.getUsername());
//
//        UserResource testAdminResource = testRealmResource.users().get(testRealmAdminUser.getId());
//        resetUserPassword(testAdminResource, PASSWORD, false);
//
//        System.out.println(" adding realm-admin role");
//        ClientRepresentation realmManagementClient = findClientByClientId(testRealmResource, "realm-management");
//        assignClientRoles(testAdminResource, realmManagementClient.getId(), "realm-admin");
//    }

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
    
}
