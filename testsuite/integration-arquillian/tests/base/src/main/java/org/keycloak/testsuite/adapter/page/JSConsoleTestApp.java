/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.adapter.page;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import java.net.URL;

/**
 *
 * @author tkyjovsk
 */
public class JSConsoleTestApp extends AbstractPageWithInjectedUrl {

    public static final String DEPLOYMENT_NAME = "js-console-example";
    public static final String CLIENT_ID = "integration-arquillian-test-apps-js-console";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Override
    public URL getInjectedUrl() {
        //EAP6 URL fix
        URL fixedUrl = createInjectedURL("js-console");
        return fixedUrl != null ? fixedUrl : url;
    }

    @FindBy(xpath = "//button[text() = 'Init']")
    private WebElement initButton;
    @FindBy(xpath = "//button[text() = 'Init with both tokens']")
    private WebElement initWithBothTokens;
    @FindBy(xpath = "//button[text() = 'Init with refresh token']")
    private WebElement initWithRefreshToken;
    @FindBy(xpath = "//button[text() = 'Init with TimeSkew']")
    private WebElement initWithTimeSkew;
    @FindBy(xpath = "//button[text() = 'Init with different realm name']")
    private WebElement initWithDifferentRealmName;
    @FindBy(xpath = "//button[text() = 'Login']")
    private WebElement logInButton;
    @FindBy(xpath = "//button[text() = 'Logout']")
    private WebElement logOutButton;
    @FindBy(xpath = "//button[text() = 'Refresh Token']")
    private WebElement refreshTokenButton;
    @FindBy(xpath = "//button[contains(text(),'Refresh Token (if <30s')]")
    private WebElement refreshTokenIfUnder30sButton;
    @FindBy(xpath = "//button[contains(text(),'Refresh Token (if <5s')]")
    private WebElement refreshTokenIfUnder5sButton;
    @FindBy(xpath = "//button[text() = 'Get Profile']")
    private WebElement getProfileButton;

    @FindBy(xpath = "//button[text() = 'Show Error Response']")
    private WebElement showErrorButton;
    @FindBy(xpath = "//button[text() = 'Show Token']")
    private WebElement showTokenButton;
    @FindBy(xpath = "//button[text() = 'Show Refresh Token']")
    private WebElement showRefreshTokenButton;
    @FindBy(xpath = "//button[text() = 'Show ID Token']")
    private WebElement showIdTokenButton;
    @FindBy(xpath = "//button[text() = 'Show Expires']")
    private WebElement showExpiresButton;
    @FindBy(xpath = "//button[text() = 'Show Details']")
    private WebElement showDetailsButton;
    @FindBy(xpath = "//button[text() = 'Create Bearer Request']")
    private WebElement createBearerRequest;
    @FindBy(xpath = "//button[text() = 'Bearer to keycloak']")
    private WebElement createBearerRequestToKeycloakButton;
    @FindBy(xpath = "//button[text() = 'Cert request']")
    private WebElement certRequestButton;
    @FindBy(xpath = "//button[text() = 'refresh timeSkew']")
    private WebElement refreshTimeSkewButton;
    @FindBy(xpath = "//button[text() = 'Create user']")
    private WebElement createUserButton;
    @FindBy(xpath = "//button[text() = 'Reentrancy callback']")
    private WebElement reentrancyCallbackButton;

    @FindBy(id = "timeSkew")
    private WebElement timeSkewValue;
    @FindBy(id = "inputField")
    private WebElement generalInput;
    @FindBy(id = "inputField2")
    private WebElement generalInput2;
    @FindBy(id = "inputField3")
    private WebElement generalInput3;
    @FindBy(xpath = "//button[text() = 'timeSkew offset']")
    private WebElement timeSkewButton;

    @FindBy(id = "flowSelect")
    private Select flowSelect;
    @FindBy(id = "responseModeSelect")
    private Select responseModeSelect;
    @FindBy(id = "onLoad")
    private Select onLoad;


    @FindBy(id = "output")
    private WebElement outputArea;

    @FindBy(id = "events")
    private WebElement eventsArea;

    public void logIn() {
        logInButton.click();
    }

    public void logOut() {
        logOutButton.click();
    }

    public void refreshToken() {
        refreshTokenButton.click();
    }

    public void refreshTokenIfUnder30s() {
        refreshTokenIfUnder30sButton.click();
    }

    public void refreshTokenIfUnder5s() {
        refreshTokenIfUnder5sButton.click();
    }

    public void getProfile() {
        getProfileButton.click();
    }

    public void setFlow(String value) {
        flowSelect.selectByValue(value);
    }

    public void setOnLoad(String value) {
        onLoad.selectByValue(value);
    }

    public void init() {
        initButton.click();
    }

    public void initWithBothTokens() {
        initWithBothTokens.click();
    }

    public void initWithRefreshToken() {
        initWithRefreshToken.click();
    }

    public void initWithTimeSkew() {
        initWithTimeSkew.click();
    }

    public void initWithDifferentRealmName() {
        initWithDifferentRealmName.click();
    }

    public void createBearerRequest() {
        createBearerRequest.click();
    }

    public void createBearerRequestToKeycloak() {
        createBearerRequestToKeycloakButton.click();
    }

    public void setResponseMode(String value) {
        responseModeSelect.selectByValue(value);
    }

    public WebElement getOutputElement() {
        return outputArea;
    }

    public WebElement getEventsElement() {
        return eventsArea;
    }

    public WebElement getInitButtonElement() {
        return initButton;
    }

    public void showErrorResponse() {
        showErrorButton.click();
    }

    public WebElement getTimeSkewValue() {
        return timeSkewValue;
    }

    public void setInput(String value) {
        generalInput.clear();
        generalInput.sendKeys(value);
    }

    public void setInput2(String value) {
        generalInput2.clear();
        generalInput2.sendKeys(value);
    }

    public void setInput3(String value) {
        generalInput3.clear();
        generalInput3.sendKeys(value);
    }

    public void setInput(int value) {
        setInput(Integer.toString(value));
    }

    public void setTimeSkew(int value) {
        setInput(value);
        timeSkewButton.click();
    }

    public void refreshTimeSkew() {
        refreshTimeSkewButton.click();
    }

    public void createUserRequest() {
        createUserButton.click();
    }

    public void sendCertRequest() {
        certRequestButton.click();
    }

    public void callReentrancyCallback() {
        reentrancyCallbackButton.click();
    }
}
