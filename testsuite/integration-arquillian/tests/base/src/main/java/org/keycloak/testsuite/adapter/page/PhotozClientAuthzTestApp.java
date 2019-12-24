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
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import org.keycloak.testsuite.util.JavascriptBrowser;
import org.keycloak.testsuite.util.UIUtils;
import org.keycloak.testsuite.util.URLUtils;
import org.keycloak.testsuite.util.javascript.JavascriptStateValidator;
import org.keycloak.testsuite.util.javascript.JavascriptTestExecutorWithAuthorization;
import org.keycloak.testsuite.util.javascript.ResponseValidator;
import org.keycloak.testsuite.util.javascript.XMLHttpRequest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.WaitUtils.pause;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class PhotozClientAuthzTestApp extends AbstractPageWithInjectedUrl {

    public static final String DEPLOYMENT_NAME = "photoz-html5-client";
    public static final int WAIT_AFTER_OPERATION = 1000;

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Drone
    @JavascriptBrowser
    protected WebDriver driver;

    @Page
    @JavascriptBrowser
    protected OIDCLogin loginPage;


    @FindBy(xpath = "//a[@ng-click = 'Identity.logout()']")
    @JavascriptBrowser
    private WebElement signOutButton;
    
    @FindBy(id = "entitlement")
    @JavascriptBrowser
    private WebElement entitlement;
    
    @FindBy(id = "entitlements")
    @JavascriptBrowser
    private WebElement entitlements;

    @FindBy(id = "get-all-resources")
    @JavascriptBrowser
    private WebElement viewAllAlbums;

    @FindBy(id = "output")
    @JavascriptBrowser
    private WebElement output;

    private JavascriptTestExecutorWithAuthorization testExecutor;
    private String apiUrl;

    public void setTestExecutorPlayground(JavascriptTestExecutorWithAuthorization executor, String apiUrl) {
        testExecutor = executor;
        this.apiUrl = apiUrl;
    }

    public void createAlbum(String name) {
        createAlbum(name, false);
    }

    public void createAlbum(String name, boolean managed) {
        createAlbum(name, managed, false, null);
    }

    public void createAlbum(String name, boolean managed, boolean invalidUser, ResponseValidator validator) {
        testExecutor.sendXMLHttpRequest(
                XMLHttpRequest.create()
                        .method("POST")
                        .url(apiUrl + "/album" + (invalidUser ? "?user=invalidUser" : ""))
                        .content("JSON.stringify(JSON.parse('{\"name\" : \"" + name + "\", \"userManaged\": " + Boolean.toString(managed) + " }'))")
                        .addHeader("Content-Type", "application/json; charset=UTF-8")
                , validator);
    }


    public void createAlbumWithInvalidUser(String name, ResponseValidator validator) {
        createAlbum(name, false, true, validator);
    }



    @Override
    public URL getInjectedUrl() {
        return this.url;
    }

    public void deleteAlbum(String name, ResponseValidator validator) {
        testExecutor.sendXMLHttpRequest(
                XMLHttpRequest.create()
                        .method("DELETE")
                        .url(apiUrl + "/album/" + name + "/") // it doesn't work without ending "/"
                , validator);
    }

    public void navigateToAdminAlbum(ResponseValidator validator) {
        testExecutor.sendXMLHttpRequest(
                XMLHttpRequest.create()
                        .method("GET")
                        .addHeader("Accept", "application/json")
                        .url(apiUrl + "/admin/album")
                , validator);
    }

    public void logOut() {
        navigateTo();
        waitUntilElement(signOutButton).is().clickable(); // Sometimes doesn't work in PhantomJS!
        clickLink(signOutButton);
    }
    
    public void requestEntitlement(JavascriptStateValidator validator) {
        testExecutor.executeAsyncScript("var callback = arguments[arguments.length - 1];" +
                "window.authorization.entitlement('photoz-restful-api', {" +
                "    \"permissions\": [" +
                "        {" +
                "            \"id\" : \"Album Resource\"" +
                "        }" +
                "    ]" +
                "}).then(function (rpt) {" +
                "    callback(JSON.stringify(jwt_decode(rpt), null, '  '));" +
                "});", validator);
    }
    
    public void requestEntitlements(JavascriptStateValidator validator) {
        testExecutor.executeAsyncScript("var callback = arguments[arguments.length - 1];" +
                "window.authorization.entitlement('photoz-restful-api', {}).then(function (rpt) {" +
                "     callback(JSON.stringify(jwt_decode(rpt), null, '  '));" +
                "});", validator);
    }

    private void waitForDenial() {
        waitUntilElement(output).text().contains("You can not access");
    }

    private void waitForNotDenial() {
        waitUntilElement(output).text().not().contains("You can not access");
    }

    public void viewAllAlbums() {
        viewAllAlbums.click();
        pause(WAIT_AFTER_OPERATION);
    }

    public void viewAlbum(String name, ResponseValidator validator) {
        testExecutor.sendXMLHttpRequest(
                XMLHttpRequest.create()
                        .method("GET")
                        .addHeader("Accept", "application/json")
                        .url(apiUrl + "/album/" + name + "/")
                , validator);
    }

    public void accountPage() {
        testExecutor.openAccountPage(null);
    }

    public void accountMyResources() {
        accountPage();
        WebElement myResources = driver.findElement(By.xpath("//a[text() = 'My Resources']"));
        waitUntilElement(myResources).is().clickable();
        myResources.click();
    }

    public void accountMyResource(String name) {
        accountMyResources();
        WebElement myResource = driver.findElement(By.id("detail-" + name));
        waitUntilElement(myResource).is().clickable();
        myResource.click();
    }

    public void accountGrantResource(String name, String requester) {
        accountMyResources();
        grantResource(name, requester);
    }

    public void grantResource(String name, String requester) {
        WebElement grantResource = driver.findElement(By.id("grant-" + name + "-" + requester));
        waitUntilElement(grantResource).is().clickable();
        grantResource.click();
    }

    public void accountGrantRemoveScope(String name, String requester, String scope) {
        accountMyResources();
        WebElement grantRemoveScope = driver.findElement(By.id("grant-remove-scope-" + name + "-" + requester + "-" + scope));
        waitUntilElement(grantRemoveScope).is().clickable();
        grantRemoveScope.click();
    }

    public void accountRevokeResource(String name, String requester) {
        accountMyResource(name);
        revokeResource(name, requester);
    }

    public void revokeResource(String name, String requester) {
        WebElement revokeResource = driver.findElement(By.id("revoke-" + name + "-" + requester));
        waitUntilElement(revokeResource).is().clickable();
        revokeResource.click();
    }

    public void accountShareResource(String name, String user) {
        accountMyResource(name);
        shareResource(user);
    }

    public void accountShareRemoveScope(String name, String user, String scope) {
        accountMyResource(name);
        
        WebElement shareRemoveScope = driver.findElement(By.id("share-remove-scope-" + name + "-" + scope));
        waitUntilElement(shareRemoveScope).is().clickable();
        shareRemoveScope.click();

        shareResource(user);
    }
    
    public void shareResource(String user) {
        WebElement userIdInput = driver.findElement(By.id("user_id"));
        UIUtils.setTextInputValue(userIdInput, user);
        pause(200); // We need to wait a bit for the form to "accept" the input (otherwise it registers the input as empty)
        waitUntilElement(userIdInput).attribute(UIUtils.VALUE_ATTR_NAME).contains(user);

        WebElement shareButton = driver.findElement(By.id("share-button"));
        waitUntilElement(shareButton).is().clickable();
        shareButton.click();
    }
    
    public void assertError() {
        assertEquals("We are sorry...", driver.findElement(By.id("kc-page-title")).getText());
    }

    public void accountDenyResource(String name) {
        accountMyResource(name);
        WebElement denyLink = driver.findElement(By.linkText("Deny"));
        waitUntilElement(denyLink).is().clickable();
        denyLink.click();
        waitForPageToLoad();
    }

    public void requestResourceProtectedAnyScope(ResponseValidator validator) {
        testExecutor.sendXMLHttpRequest(
                XMLHttpRequest.create()
                        .method("GET")
                        .url(apiUrl + "/scope-any")
                , validator);
    }

    public void requestResourceProtectedAllScope(ResponseValidator validator) {
        testExecutor.sendXMLHttpRequest(
                XMLHttpRequest.create()
                        .method("GET")
                        .url(apiUrl + "/scope-all")
                , validator);
    }

    public WebElement getOutput() {
        return output;
    }

    @Override
    public void navigateTo() {
        driver.navigate().to(toString());
        waitForPageToLoad();
    }

    @Override
    public boolean isCurrent() {
        return URLUtils.currentUrlStartsWith(toString());
    }

    public void executeScript(String script) {
        testExecutor.executeScript(script);
    }
}
