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
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.pages.ConsentPage;
import org.keycloak.testsuite.util.JavascriptBrowser;
import org.keycloak.testsuite.util.URLUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.net.URL;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
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

    @Page
    @JavascriptBrowser
    protected ConsentPage consentPage;

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

    public void createAlbum(String name) {
        createAlbum(name, false);
    }

    public void createAlbum(String name, boolean managed) {
        if (managed) {
            createAlbum(name, "save-managed-album");
        } else {
            createAlbum(name, "save-album");
        }
    }

    public void createAlbum(String name, String buttonId) {
        log.debugf("Creating album {0} with buttonId: {1}", name, buttonId);
        navigateTo();
        WebElement createAlbum = driver.findElement(By.id("create-album"));
        waitUntilElement(createAlbum).is().clickable();
        createAlbum.click();
        WebElement albumNameInput = driver.findElement(By.id("album.name"));
        waitUntilElement(albumNameInput).is().present();
        Form.setInputValue(albumNameInput, name);
        waitUntilElement(albumNameInput).attribute(Form.VALUE).contains(name);
        WebElement button = driver.findElement(By.id(buttonId));
        waitUntilElement(button).is().clickable();
        button.click();
        pause(WAIT_AFTER_OPERATION);
        if (buttonId.equals("save-album-invalid")) {
            waitForPageToLoad();
            assertThat(driver.getPageSource(), containsString("Could not register protected resource."));
        } else {
            waitUntilElement(albumNameInput).is().not().present();
        }
    }

    public void createAlbumWithInvalidUser(String name) {
        createAlbum(name, "save-album-invalid");
    }

    @Override
    public URL getInjectedUrl() {
        return this.url;
    }

    public void deleteAlbum(String name, boolean shouldBeDenied) {
        log.debugf("Deleting album {0}", name);
        WebElement delete = driver.findElement(By.id("delete-" + name));
        waitUntilElement(delete).is().clickable();
        delete.click();
        pause(WAIT_AFTER_OPERATION);
        if (shouldBeDenied) {
            waitForDenial();
        } else {
            waitUntilElement(delete).is().not().present();
        }
    }

    public void navigateToAdminAlbum(boolean shouldBeDenied) {
        log.debug("Navigating to Admin Album");
        URLUtils.navigateToUri(toString() + "/#/admin/album", true);
        
        driver.navigate().refresh(); // This is sometimes necessary for loading the new policy settings
        waitForPageToLoad();
        pause(WAIT_AFTER_OPERATION);
        if (shouldBeDenied) {
            waitForDenial();
        } else {
            waitUntilElement(output).text().equalTo("");
        }
    }

    public void logOut() {
        navigateTo();
        waitUntilElement(signOutButton).is().clickable(); // Sometimes doesn't work in PhantomJS!
        signOutButton.click();
        this.loginPage.form().waitForLoginButtonPresent();
    }
    
    public void requestEntitlement() {
        waitUntilElement(entitlement).is().clickable();
        entitlement.click();
        waitForPageToLoad();
        pause(WAIT_AFTER_OPERATION);
        pause(WAIT_AFTER_OPERATION);
    }
    
    public void requestEntitlements() {
        waitUntilElement(entitlements).is().clickable();
        entitlements.click();
        waitForPageToLoad();
        pause(WAIT_AFTER_OPERATION);
        pause(WAIT_AFTER_OPERATION);
    }

    public void login(String username, String password, String... scopes) throws InterruptedException {
        String currentUrl = this.driver.getCurrentUrl();

        if (scopes.length > 0) {
            StringBuilder scopesValue = new StringBuilder();

            for (String scope : scopes) {
                if (scopesValue.length() != 0) {
                    scopesValue.append(" ");
                }
                scopesValue.append(scope);
            }

            scopesValue.append(" openid");


            StringBuilder urlWithScopeParam = new StringBuilder(currentUrl);

            int scopeIndex = currentUrl.indexOf("scope");

            if (scopeIndex != -1) {
                // Remove scope param from url
                urlWithScopeParam.delete(scopeIndex, currentUrl.indexOf('&', scopeIndex));
                // Add scope param to the end of query
                urlWithScopeParam.append("&").append("scope=");
            }

            if (!currentUrl.contains("?")) {
                urlWithScopeParam.append("?scope=");
            }

            urlWithScopeParam.append(scopesValue);

            URLUtils.navigateToUri(urlWithScopeParam.toString(), true);
        }

        this.loginPage.form().login(username, password);
        waitForPageToLoad();//guess

        try {
            if (!isCurrent()) {
                // simple check if we are at the consent page, if so just click 'Yes'
                if (this.consentPage.isCurrent(driver)) {
                    consentPage.confirm();
                }
            }
        } catch (Exception ignore) {
            // ignore errors when checking consent page, if an error tests will also fail
        }

        pause(WAIT_AFTER_OPERATION);
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

    public void viewAlbum(String name, boolean shouldBeDenied) {
        WebElement viewalbum = driver.findElement(By.xpath("//a[text() = '" + name + "']"));
        waitUntilElement(viewalbum).is().clickable();
        viewalbum.click();
        waitForPageToLoad();
        driver.navigate().refresh(); // This is sometimes necessary for loading the new policy settings
        if (shouldBeDenied) {
            waitForDenial();
        } else {
            waitForNotDenial();
        }
        waitForPageToLoad();
        pause(WAIT_AFTER_OPERATION);
    }

    public void accountPage() {
        navigateTo();
        WebElement myAccount = driver.findElement(By.id("my-account"));
        waitUntilElement(myAccount).is().clickable();
        myAccount.click();
        waitForPageToLoad();
        pause(WAIT_AFTER_OPERATION);
    }

    public void accountMyResources() {
        accountPage();
        WebElement myResources = driver.findElement(By.xpath("//a[text() = 'My Resources']"));
        waitUntilElement(myResources).is().clickable();
        myResources.click();
        waitForPageToLoad();
        pause(WAIT_AFTER_OPERATION);
    }

    public void accountMyResource(String name) {
        accountMyResources();
        WebElement myResource = driver.findElement(By.id("detail-" + name));
        waitUntilElement(myResource).is().clickable();
        myResource.click();
        waitForPageToLoad();
        pause(WAIT_AFTER_OPERATION);
    }

    public void accountGrantResource(String name, String requester) {
        accountMyResources();
        WebElement grantResource = driver.findElement(By.id("grant-" + name + "-" + requester));
        waitUntilElement(grantResource).is().clickable();
        grantResource.click();
        waitForPageToLoad();
        pause(WAIT_AFTER_OPERATION);
    }

    public void accountGrantRemoveScope(String name, String requester, String scope) {
        accountMyResources();
        WebElement grantRemoveScope = driver.findElement(By.id("grant-remove-scope-" + name + "-" + requester + "-" + scope));
        waitUntilElement(grantRemoveScope).is().clickable();
        grantRemoveScope.click();
        waitForPageToLoad();
        pause(WAIT_AFTER_OPERATION);
    }

    public void accountRevokeResource(String name, String requester) {
        accountMyResource(name);
        WebElement revokeResource = driver.findElement(By.id("revoke-" + name + "-" + requester));
        waitUntilElement(revokeResource).is().clickable();
        revokeResource.click();
        waitForPageToLoad();
        pause(WAIT_AFTER_OPERATION);
    }

    public void accountShareResource(String name, String user) {
        accountMyResource(name);
        WebElement userIdInput = driver.findElement(By.id("user_id"));
        Form.setInputValue(userIdInput, user);
        pause(200); // We need to wait a bit for the form to "accept" the input (otherwise it registers the input as empty)
        waitUntilElement(userIdInput).attribute(Form.VALUE).contains(user);
        
        WebElement shareButton = driver.findElement(By.id("share-button"));
        waitUntilElement(shareButton).is().clickable();
        shareButton.click();
        waitForPageToLoad();
        pause(WAIT_AFTER_OPERATION);
    }

    public void accountShareRemoveScope(String name, String user, String scope) {
        accountMyResource(name);
        
        WebElement userIdInput = driver.findElement(By.id("user_id"));
        Form.setInputValue(userIdInput, user);
        pause(200); // We need to wait a bit for the form to "accept" the input (otherwise it registers the input as empty)
        waitUntilElement(userIdInput).attribute(Form.VALUE).contains(user);
        
        WebElement shareRemoveScope = driver.findElement(By.id("share-remove-scope-" + name + "-" + scope));
        waitUntilElement(shareRemoveScope).is().clickable();
        shareRemoveScope.click();
        waitForPageToLoad();
        
        WebElement shareButton = driver.findElement(By.id("share-button"));
        waitUntilElement(shareButton).is().clickable();
        shareButton.click();
        
        waitForPageToLoad();
        pause(WAIT_AFTER_OPERATION);
    }

    public void accountDenyResource(String name) {
        accountMyResource(name);
        WebElement denyLink = driver.findElement(By.linkText("Deny"));
        waitUntilElement(denyLink).is().clickable();
        denyLink.click();
        waitForPageToLoad();
        pause(WAIT_AFTER_OPERATION);
    }

    public void requestResourceProtectedAnyScope(boolean shouldBeDenied) {
        navigateTo();
        WebElement requestPathWithAnyProtectedScope = driver.findElement(By.id("requestPathWithAnyProtectedScope"));
        waitUntilElement(requestPathWithAnyProtectedScope).is().clickable();
        requestPathWithAnyProtectedScope.click();
        if (shouldBeDenied) waitForDenial();
        pause(WAIT_AFTER_OPERATION);
    }

    public void requestResourceProtectedAllScope(boolean shouldBeDenied) {
        navigateTo();
        WebElement requestPathWithAllProtectedScope = driver.findElement(By.id("requestPathWithAllProtectedScope"));
        waitUntilElement(requestPathWithAllProtectedScope).is().clickable();
        requestPathWithAllProtectedScope.click();
        if (shouldBeDenied) waitForDenial();
        pause(WAIT_AFTER_OPERATION);
    }

    public WebElement getOutput() {
        return output;
    }

    @Override
    public void navigateTo(boolean waitForMatch) {
        super.navigateTo(waitForMatch);
        pause(WAIT_AFTER_OPERATION);
    }

    @Override
    public boolean isCurrent() {
        return URLUtils.currentUrlStartWith(toString());
    }
}
