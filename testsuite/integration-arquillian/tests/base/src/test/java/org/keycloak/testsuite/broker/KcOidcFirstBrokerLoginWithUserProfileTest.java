/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.broker;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;
import static org.keycloak.testsuite.forms.VerifyProfileTest.ATTRIBUTE_DEPARTMENT;
import static org.keycloak.testsuite.forms.VerifyProfileTest.PERMISSIONS_ADMIN_EDITABLE;
import static org.keycloak.testsuite.forms.VerifyProfileTest.PERMISSIONS_ALL;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.Profile;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.forms.RegisterWithUserProfileTest;
import org.keycloak.testsuite.forms.VerifyProfileTest;
import org.keycloak.testsuite.util.ClientScopeBuilder;
import org.openqa.selenium.By;

/**
 *
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
@EnableFeature(value = Profile.Feature.DECLARATIVE_USER_PROFILE)
@AuthServerContainerExclude(AuthServerContainerExclude.AuthServer.REMOTE)
public class KcOidcFirstBrokerLoginWithUserProfileTest extends KcOidcFirstBrokerLoginTest {

    @Override
    @Before
    public void beforeBrokerTest() {
        super.beforeBrokerTest();
        enableDynamicUserProfile();
    }

    @Test
    public void testDisplayName() {

        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\",\"displayName\":\"${firstName}\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", \"displayName\" : \"Department\", " + PERMISSIONS_ALL + ", \"required\":{}}"
                + "]}");

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();

        //assert field names
        // i18n replaced
        Assert.assertEquals("First name", updateAccountInformationPage.getLabelForField("firstName"));
        // attribute name used if no display name set
        Assert.assertEquals("lastName", updateAccountInformationPage.getLabelForField("lastName"));
        // direct value in display name
        Assert.assertEquals("Department", updateAccountInformationPage.getLabelForField("department"));
    }

    @Test
    public void testAttributeGrouping() {

        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"lastName\"," + VerifyProfileTest.PERMISSIONS_ALL + "},"
                + "{\"name\": \"username\", " + VerifyProfileTest.PERMISSIONS_ALL + "},"
                + "{\"name\": \"firstName\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"department\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\":{}, \"group\": \"company\"},"
                + "{\"name\": \"email\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"group\": \"contact\"}"
                + "], \"groups\": ["
                + "{\"name\": \"company\", \"displayDescription\": \"Company field desc\" },"
                + "{\"name\": \"contact\" }"
                + "]}");

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();

        //assert fields location in form
        String htmlFormId = "kc-idp-review-profile-form";

        //assert fields and groups location in form
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(1) > div:nth-child(2) > input#lastName")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(2) > div:nth-child(2) > input#username")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(3) > div:nth-child(2) > input#firstName")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(4) > div:nth-child(1) > label#header-company")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(4) > div:nth-child(2) > label#description-company")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(5) > div:nth-child(2) > input#department")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(6) > div:nth-child(1) > label#header-contact")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(7) > div:nth-child(2) > input#email")
                ).isDisplayed()
        );
    }

    @Test
    public void testAttributeGuiOrder() {

        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"lastName\"," + VerifyProfileTest.PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\":{}},"
                + "{\"name\": \"username\", " + VerifyProfileTest.PERMISSIONS_ALL + "},"
                + "{\"name\": \"firstName\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"email\", " + VerifyProfileTest.PERMISSIONS_ALL + "}"
                + "]}");

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();

        //assert fields location in form
        String htmlFormId = "kc-idp-review-profile-form";
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(1) > div:nth-child(2) > input#lastName")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(2) > div:nth-child(2) > input#department")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(3) > div:nth-child(2) > input#username")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(4) > div:nth-child(2) > input#firstName")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(5) > div:nth-child(2) > input#email")
                ).isDisplayed()
        );
    }

    @Test
    public void testAttributeInputTypes() {

        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);

        setUserProfileConfiguration("{\"attributes\": ["
                + RegisterWithUserProfileTest.UP_CONFIG_PART_INPUT_TYPES
                + "]}");

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();

        RegisterWithUserProfileTest.assertFieldTypes(driver);
    }

    @Test
    public void testDynamicUserProfileReviewWhenMissing_requiredReadOnlyAttributeDoesnotForceUpdate() {

        updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", " + PERMISSIONS_ADMIN_EDITABLE + ", \"required\":{}}"
                + "]}");

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithBroker(bc);

        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();
    }

    @Test
    public void testDynamicUserProfileReviewWhenMissing_requiredButNotSelectedByScopeAttributeDoesnotForceUpdate() {

        addDepartmentScopeIntoRealm();

        updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", " + PERMISSIONS_ALL + ", \"required\":{}, \"selector\":{\"scopes\":[\"department\"]}}"
                + "]}");

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithBroker(bc);

        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();
    }

    @Test
    public void testDynamicUserProfileReviewWhenMissing_requiredAndSelectedByScopeAttributeForcesUpdate() {

        updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);

        //we use 'profile' scope which is requested by default
        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", " + PERMISSIONS_ALL + ", \"required\":{}, \"selector\":{\"scopes\":[\"profile\"]}}"
                + "]}");

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
    }

    @Test
    public void testDynamicUserProfileReview_requiredReadOnlyAttributeNotRenderedAndNotBlockingProcess() {

        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", " + PERMISSIONS_ADMIN_EDITABLE + ", \"required\":{}}"
                + "]}");

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();

        Assert.assertFalse(updateAccountInformationPage.isDepartmentPresent());


        updateAccountInformationPage.updateAccountInformation( "requiredReadOnlyAttributeNotRenderedAndNotBlockingRegistration", "requiredReadOnlyAttributeNotRenderedAndNotBlockingRegistration@email", "FirstAA", "LastAA");

        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();
    }

    @Test
    public void testDynamicUserProfileReview_attributeRequiredAndSelectedByScopeMustBeSet() {

        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);

        //we use 'profile' scope which is requested by default
        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{}, \"selector\":{\"scopes\":[\"profile\"]}}"
                + "]}");

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();

        //check required validation works
        updateAccountInformationPage.updateAccountInformation( "attributeRequiredAndSelectedByScopeMustBeSet", "attributeRequiredAndSelectedByScopeMustBeSet@email", "FirstAA", "LastAA", "");
        updateAccountInformationPage.assertCurrent();

        updateAccountInformationPage.updateAccountInformation( "attributeRequiredAndSelectedByScopeMustBeSet", "attributeRequiredAndSelectedByScopeMustBeSet@email", "FirstAA", "LastAA", "DepartmentAA");

        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();

        UserRepresentation user = VerifyProfileTest.getUserByUsername(testRealm(),"attributeRequiredAndSelectedByScopeMustBeSet");
        assertEquals("FirstAA", user.getFirstName());
        assertEquals("LastAA", user.getLastName());
        assertEquals("DepartmentAA", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testDynamicUserProfileReview_attributeNotRequiredAndSelectedByScopeCanBeIgnored() {

        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);

        //we use 'profile' scope which is requested by default
        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"selector\":{\"scopes\":[\"profile\"]}}"
                + "]}");

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();

        Assert.assertTrue(updateAccountInformationPage.isDepartmentPresent());
        updateAccountInformationPage.updateAccountInformation( "attributeNotRequiredAndSelectedByScopeCanBeIgnored", "attributeNotRequiredAndSelectedByScopeCanBeIgnored@email", "FirstAA", "LastAA");

        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();

        UserRepresentation user = VerifyProfileTest.getUserByUsername(testRealm(),"attributeNotRequiredAndSelectedByScopeCanBeIgnored");
        assertEquals("FirstAA", user.getFirstName());
        assertEquals("LastAA", user.getLastName());
        assertThat(StringUtils.isEmpty(user.firstAttribute(ATTRIBUTE_DEPARTMENT)), is(true));
    }

    @Test
    public void testDynamicUserProfileReview_attributeNotRequiredAndSelectedByScopeCanBeSet() {

        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);

        //we use 'profile' scope which is requested by default
        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"selector\":{\"scopes\":[\"profile\"]}}"
                + "]}");

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();

        Assert.assertTrue(updateAccountInformationPage.isDepartmentPresent());
        updateAccountInformationPage.updateAccountInformation( "attributeNotRequiredAndSelectedByScopeCanBeSet", "attributeNotRequiredAndSelectedByScopeCanBeSet@email", "FirstAA", "LastAA","Department AA");

        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();

        UserRepresentation user = VerifyProfileTest.getUserByUsername(testRealm(),"attributeNotRequiredAndSelectedByScopeCanBeSet");
        assertEquals("FirstAA", user.getFirstName());
        assertEquals("LastAA", user.getLastName());
        assertEquals("Department AA", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testDynamicUserProfileReview_attributeRequiredButNotSelectedByScopeIsNotRenderedAndNotBlockingProcess() {

        addDepartmentScopeIntoRealm();

        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{}, \"selector\":{\"scopes\":[\"department\"]}}"
                + "]}");

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();

        Assert.assertFalse(updateAccountInformationPage.isDepartmentPresent());
        updateAccountInformationPage.updateAccountInformation( "attributeRequiredButNotSelectedByScopeIsNotRenderedAndNotBlockingRegistration", "attributeRequiredButNotSelectedByScopeIsNotRenderedAndNotBlockingRegistration@email", "FirstAA", "LastAA");

        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();

        UserRepresentation user = VerifyProfileTest.getUserByUsername(testRealm(),"attributeRequiredButNotSelectedByScopeIsNotRenderedAndNotBlockingRegistration");
        assertEquals("FirstAA", user.getFirstName());
        assertEquals("LastAA", user.getLastName());
        assertEquals(null, user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    public void addDepartmentScopeIntoRealm() {
        testRealm().clientScopes().create(ClientScopeBuilder.create().name("department").protocol("openid-connect").build());
    }

    protected void setUserProfileConfiguration(String configuration) {
        VerifyProfileTest.setUserProfileConfiguration(testRealm(), configuration);
    }

    private RealmResource testRealm() {
        return adminClient.realm(bc.consumerRealmName());
    }

}