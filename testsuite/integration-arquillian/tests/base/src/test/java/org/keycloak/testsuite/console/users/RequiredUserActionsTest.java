package org.keycloak.testsuite.console.users;

import static org.jboss.arquillian.graphene.Graphene.waitGui;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import org.keycloak.testsuite.auth.page.account.Account;
import org.keycloak.testsuite.auth.page.login.UpdateAccount;
import org.keycloak.testsuite.auth.page.login.UpdatePassword;
import org.keycloak.testsuite.console.page.authentication.RequiredActions;
import org.keycloak.testsuite.console.page.users.UserAttributes;

import static org.keycloak.testsuite.model.RequiredUserAction.TERMS_AND_CONDITIONS;
import static org.keycloak.testsuite.model.RequiredUserAction.UPDATE_PASSWORD;
import static org.keycloak.testsuite.model.RequiredUserAction.UPDATE_PROFILE;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrlStartsWith;

import org.keycloak.testsuite.util.SeleniumUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 * @author mhajas
 */
public class RequiredUserActionsTest extends AbstractUserTest {

    @Page
    private UserAttributes userAttributes;

    @Page
    private Account testRealmAccount;

    @Page
    private UpdateAccount testRealmUpdateAccount;

    @Page
    private UpdatePassword testRealmUpdatePassword;

    @Page
    private RequiredActions requiredActions;

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
    public void updatePassword() {
        userAttributes.form().addRequiredAction(UPDATE_PASSWORD.getActionName());
        userAttributes.form().save();
        assertFlashMessageSuccess();

        testRealmAccount.navigateTo();

        testRealmLogin.form().login(testRealmUser);
        waitForFeedbackText("You need to change your password to activate your account.");

        testRealmUpdatePassword.updatePasswords(null, null);
        waitForFeedbackText("Please specify password.");

        testRealmUpdatePassword.updatePasswords(PASSWORD, null);
        waitForFeedbackText("Passwords don't match.");

        testRealmUpdatePassword.updatePasswords(PASSWORD, PASSWORD + "-mismatch");
        waitForFeedbackText("Passwords don't match.");

        testRealmUpdatePassword.updatePasswords(PASSWORD, PASSWORD);
        assertCurrentUrlStartsWith(testRealmAccount);
    }

    @Test
    public void updateProfile() {
        userAttributes.form().addRequiredAction(UPDATE_PROFILE.getActionName());
        userAttributes.form().save();
        assertFlashMessageSuccess();

        testRealmAccount.navigateTo();

        testRealmLogin.form().login(testRealmUser);
        waitForFeedbackText("You need to update your user profile to activate your account.");

        testRealmUser.setEmail(null);
        testRealmUser.setFirstName(null);
        testRealmUser.setLastName(null);
        testRealmUpdateAccount.updateAccount(testRealmUser);
        waitForFeedbackText("Please specify email.");

        testRealmUser.setEmail("test@email.test");
        testRealmUpdateAccount.updateAccount(testRealmUser);
        waitForFeedbackText("Please specify first name.");

        testRealmUser.setFirstName("test");
        testRealmUpdateAccount.updateAccount(testRealmUser);
        waitForFeedbackText("Please specify last name.");

        testRealmUser.setLastName("user");
        testRealmUpdateAccount.updateAccount(testRealmUser);
        assertCurrentUrlStartsWith(testRealmAccount);
    }

    @Test
    public void termsAndConditions() {
        requiredActions.navigateTo();
        requiredActions.clickTermsAndConditionEnabled();

        manage().users();
        users.table().viewAllUsers();
        users.table().clickUser(testRealmUser.getUsername());

        userAttributes.form().addRequiredAction(TERMS_AND_CONDITIONS.getActionName());
        userAttributes.form().save();
        assertFlashMessageSuccess();

        testRealmAccount.navigateTo();

        testRealmLogin.form().login(testRealmUser);

        driver.findElement(By.xpath("//div[@id='kc-header-wrapper' and text()[contains(.,'Terms and Conditions')]]"));
    }




}
