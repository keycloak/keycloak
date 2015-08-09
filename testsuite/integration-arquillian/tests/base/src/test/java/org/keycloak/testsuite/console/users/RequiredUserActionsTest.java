package org.keycloak.testsuite.console.users;

import static org.jboss.arquillian.graphene.Graphene.waitGui;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import org.keycloak.testsuite.auth.page.account.Account;
import org.keycloak.testsuite.auth.page.login.UpdateAccount;
import org.keycloak.testsuite.auth.page.login.UpdatePassword;
import org.keycloak.testsuite.console.page.users.UserAttributes;
import static org.keycloak.testsuite.model.RequiredUserAction.UPDATE_PASSWORD;
import static org.keycloak.testsuite.model.RequiredUserAction.UPDATE_PROFILE;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrlStartsWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class RequiredUserActionsTest extends AbstractUserTest {

    @Page
    private UserAttributes userAttrinbutes;

    @Page
    private Account testRealmAccount;

    @Page
    private UpdateAccount testRealmUpdateAccount;
    @Page
    private UpdatePassword testRealmUpdatePassword;

    @FindBy(css = "kc-feedback-text")
    protected WebElement feedbackText;

    public void waitForFeedbackText(String text) {
        waitGui().until().element(By.className("kc-feedback-text"))
                .text().contains(text);
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmAccount.setAuthRealm(testRealm);
        testRealmUpdateAccount.setAuthRealm(testRealm);
        testRealmUpdatePassword.setAuthRealm(testRealm);
    }

    @Before
    public void beforeRequiredActionsTest() {
        users.table().viewAllUsers();
        users.table().clickUser(testRealmUser.getUsername());
    }

    @Test
    public void changePassword() {
        userAttrinbutes.form().addRequiredAction(UPDATE_PASSWORD.getActionName());
        userAttrinbutes.form().save();
        assertFlashMessageSuccess();

        testRealmAccount.navigateTo();

        testRealmLogin.form().login(testRealmUser);
        waitForFeedbackText("You need to change your password to activate your account.");

        testRealmUpdatePassword.updateForm().setPasswords(null, null);
        testRealmUpdatePassword.submit();
        waitForFeedbackText("Please specify password.");

        testRealmUpdatePassword.updateForm().setPasswords(PASSWORD, null);
        testRealmUpdatePassword.submit();
        waitForFeedbackText("Passwords don't match.");

        testRealmUpdatePassword.updateForm().setPasswords(PASSWORD, PASSWORD + "-mismatch");
        testRealmUpdatePassword.submit();
        waitForFeedbackText("Passwords don't match.");

        testRealmUpdatePassword.updateForm().setPasswords(PASSWORD, PASSWORD);
        testRealmUpdatePassword.submit();
        assertCurrentUrlStartsWith(testRealmAccount);
    }

    @Test
    public void updateProfile() {
        userAttrinbutes.form().addRequiredAction(UPDATE_PROFILE.getActionName());
        userAttrinbutes.form().save();
        assertFlashMessageSuccess();

        testRealmAccount.navigateTo();

        testRealmLogin.form().login(testRealmUser);
        waitForFeedbackText("You need to update your user profile to activate your account.");

        testRealmUser.setEmail(null);
        testRealmUser.setFirstName(null);
        testRealmUser.setLastName(null);
        testRealmUpdateAccount.updateForm().setValues(testRealmUser);
        testRealmUpdateAccount.submit();
        waitForFeedbackText("Please specify email.");

        testRealmUser.setEmail("test@email.test");
        testRealmUpdateAccount.updateForm().setValues(testRealmUser);
        testRealmUpdateAccount.submit();
        waitForFeedbackText("Please specify first name.");

        testRealmUser.setFirstName("test");
        testRealmUpdateAccount.updateForm().setValues(testRealmUser);
        testRealmUpdateAccount.submit();
        waitForFeedbackText("Please specify last name.");

        testRealmUser.setLastName("user");
        testRealmUpdateAccount.updateForm().setValues(testRealmUser);
        testRealmUpdateAccount.submit();
        assertCurrentUrlStartsWith(testRealmAccount);
    }

}
