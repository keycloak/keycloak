package org.keycloak.testframework.ui.page;

import java.util.LinkedHashMap;
import java.util.Map;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LoginUpdateProfilePage extends AbstractLoginPage {

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(name = "firstName")
    private WebElement firstNameInput;

    @FindBy(name = "lastName")
    private WebElement lastNameInput;

    @FindBy(name = "email")
    private WebElement emailInput;

    @FindBy(name = "department")
    private WebElement departmentInput;

    @FindBy(css = "input[type=\"submit\"]")
    private WebElement submitButton;

    @FindBy(name = "cancel-aia")
    private WebElement cancelAIAButton;

    @FindBy(css = "div[class^='pf-v5-c-alert'], div[class^='alert-error']")
    private WebElement loginAlertErrorMessage;

    private final UpdateProfileErrors errorsPage;

    public LoginUpdateProfilePage(ManagedWebDriver driver) {
        super(driver);
        this.errorsPage = new UpdateProfileErrors(driver);
    }

    public void update(String firstName, String lastName) {
        prepareUpdate().firstName(firstName).lastName(lastName).submit();
    }

    public void update(String firstName, String lastName, String email) {
        prepareUpdate().firstName(firstName).lastName(lastName).email(email).submit();
    }

    public void update(Map<String, String> attributes) {
        prepareUpdate().otherProfileAttribute(attributes).submit();
    }

    public Update prepareUpdate() {
        return new Update(this);
    }

    public void cancel() {
        cancelAIAButton.click();
    }

    public String getAlertError() {
        try {
            return loginAlertErrorMessage.getText();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public String getUsername() {
        return usernameInput.getAttribute("value");
    }

    public String getFirstName() {
        return firstNameInput.getAttribute("value");
    }

    public String getLastName() {
        return lastNameInput.getAttribute("value");
    }

    public String getEmail() {
        return emailInput.getAttribute("value");
    }

    public String getDepartment() {
        return departmentInput.getAttribute("value");
    }

    public boolean isDepartmentEnabled() {
        return departmentInput.isEnabled();
    }

    public UpdateProfileErrors getInputErrors() {
        return errorsPage;
    }

    public String getLabelForField(String fieldId) {
        return driver.findElement(By.cssSelector("label[for=" + fieldId + "]")).getText().replaceAll("\\s\\*$", "");
    }

    public WebElement getElementById(String fieldId) {
        try {
            return driver.findElement(By.id(fieldId));
        } catch (NoSuchElementException ignore) {
            return null;
        }
    }

    public boolean isUsernamePresent() {
        try {
            return driver.findElement(By.id("username")).isDisplayed();
        } catch (NoSuchElementException nse) {
            return false;
        }
    }

    public boolean isEmailInputPresent() {
        try {
            return emailInput.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public boolean isDepartmentPresent() {
        try {
            isDepartmentEnabled();
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public boolean isCancelDisplayed() {
        try {
            return cancelAIAButton.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public void setAttribute(String elementId, String value) {
        WebElement element = getElementById(elementId);

        if (element != null) {
            element.clear();
            element.sendKeys(value);
        }
    }

    public void clickAddAttributeValue(String elementId) {
        WebElement element = getElementById("kc-add-" + elementId);

        if (element != null) {
            element.click();
        }
    }

    public void clickRemoveAttributeValue(String elementId) {
        WebElement element = getElementById("kc-remove-" + elementId);

        if (element != null) {
            element.click();
        }
    }

    public String getAttribute(String elementId) {
        WebElement element = getElementById(elementId);

        if (element != null) {
            return element.getAttribute("value");
        }

        return null;
    }

    @Override
    public String getExpectedPageId() {
        return "login-login-update-profile";
    }

    public static class Update {
        private final LoginUpdateProfilePage page;
        private String username;
        private String firstName;
        private String lastName;
        private String department;
        private String email;
        private final Map<String, String> other = new LinkedHashMap<>();

        protected Update(LoginUpdateProfilePage page) {
            this.page = page;
        }

        public Update username(String username) {
            this.username = username;
            return this;
        }

        public Update firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Update lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Update department(String department) {
            this.department = department;
            return this;
        }

        public Update email(String email) {
            this.email = email;
            return this;
        }

        public Update otherProfileAttribute(Map<String, String> attributes) {
            other.putAll(attributes);
            return this;
        }

        public void submit() {
            if (username != null) {
                page.usernameInput.clear();
                page.usernameInput.sendKeys(username);
            }
            if (firstName != null) {
                page.firstNameInput.clear();
                page.firstNameInput.sendKeys(firstName);
            }
            if (lastName != null) {
                page.lastNameInput.clear();
                page.lastNameInput.sendKeys(lastName);
            }

            if (department != null) {
                page.departmentInput.clear();
                page.departmentInput.sendKeys(department);
            }

            if (email != null) {
                page.emailInput.clear();
                page.emailInput.sendKeys(email);
            }

            for (Map.Entry<String, String> entry : other.entrySet()) {
                WebElement el = page.driver.findElement(By.id(entry.getKey()));
                if (el != null) {
                    el.clear();
                    el.sendKeys(entry.getValue());
                }
            }

            page.submitButton.click();
        }
    }

    // For managing input errors
    public static class UpdateProfileErrors {

        private final ManagedWebDriver driver;

        public UpdateProfileErrors(ManagedWebDriver driver) {
            this.driver = driver;
        }

        private String getTextById(String id) {
            try {
                return driver.findElement(By.id(id)).getText();
            } catch (NoSuchElementException e) {
                return null;
            }
        }

        public String getFirstNameError() {
            String text = getTextById("input-error-firstname");
            if (text != null) {
                return text;
            }
            return getTextById("input-error-firstName");
        }

        public String getLastNameError() {
            String text = getTextById("input-error-lastname");
            if (text != null) {
                return text;
            }
            return getTextById("input-error-lastName");
        }

        public String getEmailError() {
            return getTextById("input-error-email");
        }

        public String getUsernameError() {
            return getTextById("input-error-username");
        }
    }
}
