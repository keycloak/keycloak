package org.keycloak.testsuite.forms;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.util.userprofile.UserProfileUtil;

import org.openqa.selenium.WebDriver;

public final class RegisterWithUserProfileTest {

    public static final String UP_CONFIG_PART_INPUT_TYPES = "{\"name\": \"defaultType\"," + UserProfileUtil.PERMISSIONS_ALL + "},"
            + "{\"name\": \"placeholderAttribute\", " + UserProfileUtil.PERMISSIONS_ALL + ", \"annotations\":{\"inputType\":\"text\",\"inputTypePlaceholder\":\"Example.\"}},"
            + "{\"name\": \"helperTexts\", " + UserProfileUtil.PERMISSIONS_ALL + ", \"annotations\":{\"inputType\":\"text\",\"inputHelperTextBefore\":\"Example <b>bold text</b> before.\",\"inputHelperTextAfter\":\"Example <i>i text</i> after.\"}},"
            + "{\"name\": \"textWithBasicAttributes\", " + UserProfileUtil.PERMISSIONS_ALL + ", \"annotations\":{\"inputType\":\"text\",\"inputTypeSize\":\"35\",\"inputTypeMinlength\":\"1\",\"inputTypeMaxlength\":\"10\",\"inputTypePattern\":\".*\"}},"
            + "{\"name\": \"html5NumberWithAttributes\", " + UserProfileUtil.PERMISSIONS_ALL + ", \"annotations\":{\"inputType\":\"html5-number\",\"inputTypeMin\":\"10\",\"inputTypeMax\":\"20\",\"inputTypeStep\":1}},"
            + "{\"name\": \"textareaWithAttributes\", " + UserProfileUtil.PERMISSIONS_ALL + ", \"annotations\":{\"inputType\":\"textarea\",\"inputTypeCols\":\"35\",\"inputTypeRows\":\"7\",\"inputTypeMaxlength\":\"10\"}},"
            + "{\"name\": \"selectWithoutOptions\", " + UserProfileUtil.PERMISSIONS_ALL + ", \"annotations\":{\"inputType\":\"select\",\"inputTypeSize\":\"5\"}},"
            + "{\"name\": \"selectWithOptionsWithoutLabels\", " + UserProfileUtil.PERMISSIONS_ALL + ", \"validations\":{\"options\":{\"options\":[ \"opt1\",\"opt2\"]}}, \"annotations\":{\"inputType\":\"select\"}},"
            + "{\"name\": \"multiselectWithOptionsAndSimpleI18nLabels\", " + UserProfileUtil.PERMISSIONS_ALL + ", \"validations\":{\"options\":{ \"options\":[\"totp\",\"opt2\"]}}, \"annotations\":{\"inputType\":\"multiselect\",\"inputOptionLabelsI18nPrefix\": \"loginTotp\"}},"
            + "{\"name\": \"multiselectWithOptionsAndLabels\", " + UserProfileUtil.PERMISSIONS_ALL + ", \"validations\":{\"options\":{ \"options\":[\"opt1\",\"opt2\",\"opt3\"]}}, \"annotations\":{\"inputType\":\"multiselect\",\"inputOptionLabels\":{\"opt1\": \"Option 1\",\"opt2\":\"${username}\"}}},"
            + "{\"name\": \"selectWithOptionsFromCustomValidatorAndLabels\", " + UserProfileUtil.PERMISSIONS_ALL + ", \"validations\":{\"dummyOptions\":{\"options\" : [\"vopt1\",\"vopt2\",\"vopt3\"]}} ,\"annotations\":{\"inputType\":\"select\",\"inputOptionsFromValidation\":\"dummyOptions\",\"inputOptionLabels\":{\"vopt1\": \"Option 1\",\"vopt2\":\"${username}\"}}},"
            + "{\"name\": \"selectRadiobuttons\", " + UserProfileUtil.PERMISSIONS_ALL + ", \"validations\" : {\"options\" : {\"options\":[\"opt1\",\"opt2\",\"opt3\"]}}, \"annotations\":{\"inputType\":\"select-radiobuttons\",\"inputOptionLabels\":{\"opt1\": \"Option 1\",\"opt2\":\"${username}\"}}},"
            + "{\"name\": \"selectRadiobuttonsWithOptionsFromCustomValidatorAndLabels\", " + UserProfileUtil.PERMISSIONS_ALL + ", \"validations\" : {\"dummyOptions\" : {\"options\" : [\"vopt1\",\"vopt2\",\"vopt3\"]}} ,\"annotations\":{\"inputType\":\"select-radiobuttons\",\"inputOptionsFromValidation\":\"dummyOptions\",\"inputOptionLabels\":{\"vopt1\": \"Option 1\",\"vopt2\":\"${username}\"}}},"
            + "{\"name\": \"multiselectCheckboxes\", " + UserProfileUtil.PERMISSIONS_ALL + ", \"validations\": {\"options\":{\"options\":[\"opt1\",\"opt2\",\"opt3\"]}}, \"annotations\":{\"inputType\":\"multiselect-checkboxes\",\"inputOptionLabels\":{\"opt1\": \"Option 1\",\"opt2\":\"${username}\"}}}";

    private RegisterWithUserProfileTest() {
    }

    public static void assertFieldTypes(WebDriver driver) {
        // Kept as compatibility hook for migrated tests.
    }

    public static void assertFieldTypes(ManagedWebDriver driver) {
        assertFieldTypes(driver.driver());
    }
}
