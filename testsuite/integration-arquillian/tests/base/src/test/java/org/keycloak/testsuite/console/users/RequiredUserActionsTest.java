package org.keycloak.testsuite.console.users;

import static org.jboss.arquillian.graphene.Graphene.waitGui;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.auth.page.login.UpdateAccount;
import org.keycloak.testsuite.console.page.users.UserAttributes;
import static org.keycloak.testsuite.model.RequiredUserAction.UPDATE_PASSWORD;
import static org.keycloak.testsuite.model.RequiredUserAction.UPDATE_PROFILE;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrlDoesntStartWith;
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
    private UpdateAccount testRealmUpdateAccount;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmUpdateAccount.setAuthRealm(testRealm);
    }

    @Before
    public void beforeRequiredActionsTest() {
        users.clickUser(testRealmUser.getUsername());
    }

    @Test
    public void testChangePasswordRequiredUserAction() {
        userAttrinbutes.form().setRequiredActions(null);
        userAttrinbutes.form().addRequiredAction(UPDATE_PASSWORD.getActionName());
        userAttrinbutes.form().save();
        assertFlashMessageSuccess();

        loginToTestRealmConsoleAs(testRealmUser);

        testRealmLogin.form().login(testRealmUser);
        waitGui().until()
                .element(By.className("kc-feedback-text"))
                .text()
                .equalTo("You need to change your password to activate your account.");
    }

    @Test
    public void testUpdateProfileRequiredUserAction() {
        userAttrinbutes.form().setRequiredActions(null);
        userAttrinbutes.form().addRequiredAction(UPDATE_PROFILE.getActionName());
        userAttrinbutes.form().save();
        assertFlashMessageSuccess();

        loginToTestRealmConsoleAs(testRealmUser);

        testRealmLogin.form().login(testRealmUser);
        waitForFeedbackText("You need to update your user profile to activate your account.");

        testRealmUpdateAccount.submit();
        waitForFeedbackText("Please specify email.");

        testRealmUpdateAccount.updateForm().setEmail(testRealmUser.getEmail());
        testRealmUpdateAccount.submit();
        waitForFeedbackText("Please specify first name.");

        testRealmUpdateAccount.updateForm().setFirstName(testRealmUser.getFirstName());
        testRealmUpdateAccount.submit();
        waitForFeedbackText("Please specify last name.");

        testRealmUpdateAccount.updateForm().setLastName(testRealmUser.getLastName());
        testRealmUpdateAccount.submit();
        
        assertCurrentUrlDoesntStartWith(testRealmAdminConsole);
    }

    @FindBy(css = "kc-feedback-text")
    protected WebElement feedbackText;

    public void waitForFeedbackText(String text) {
        waitGui().until().element(feedbackText).text().equalTo(text);
    }

}
