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
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.pages.ConsentPage;
import org.keycloak.testsuite.util.URLUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.net.URL;

import static org.keycloak.testsuite.util.WaitUtils.pause;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class PhotozClientAuthzTestApp extends AbstractPageWithInjectedUrl {

    public static final String DEPLOYMENT_NAME = "photoz-html5-client";
    public static final int WAIT_AFTER_OPERATION = 2000;

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Page
    protected OIDCLogin loginPage;

    @Page
    protected ConsentPage consentPage;

    @FindBy(xpath = "//a[@ng-click = 'Identity.logout()']")
    private WebElement signOutButton;
    
    @FindBy(id = "entitlement")
    private WebElement entitlement;
    
    @FindBy(id = "entitlements")
    private WebElement entitlements;

    @FindBy(id = "output")
    private WebElement output;
    
    public void createAlbum(String name) {
        createAlbum(name, "save-album");
    }

    public void createAlbum(String name, String buttonId) {
        navigateTo();
        this.driver.findElement(By.id("create-album")).click();
        Form.setInputValue(this.driver.findElement(By.id("album.name")), name);
        pause(200); // We need to wait a bit for the form to "accept" the input (otherwise it registers the input as empty)
        this.driver.findElement(By.id(buttonId)).click();
        pause(WAIT_AFTER_OPERATION);
    }

    public void createAlbumWithInvalidUser(String name) {
        createAlbum(name, "save-album-invalid");
    }

    @Override
    public URL getInjectedUrl() {
        return this.url;
    }

    public void deleteAlbum(String name) {
        driver.findElements(By.xpath("//a[text()='" + name + "']/following-sibling::a[text()='X']")).forEach(WebElement::click);
        pause(WAIT_AFTER_OPERATION);
    }

    public void navigateToAdminAlbum() {
        URLUtils.navigateToUri(toString() + "/#/admin/album", true);
        driver.navigate().refresh(); // This is sometimes necessary for loading the new policy settings
        waitForPageToLoad();
        pause(WAIT_AFTER_OPERATION);
    }

    public void logOut() {
        waitUntilElement(signOutButton); // Sometimes doesn't work in PhantomJS!
        signOutButton.click();
        pause(WAIT_AFTER_OPERATION);
    }
    
    public void requestEntitlement() {
        entitlement.click();
        pause(WAIT_AFTER_OPERATION);
        pause(WAIT_AFTER_OPERATION);
    }
    
    public void requestEntitlements() {
        entitlements.click();
        pause(WAIT_AFTER_OPERATION);
        pause(WAIT_AFTER_OPERATION);
    }

    public void login(String username, String password, String... scopes) throws InterruptedException {
        if (this.driver.getCurrentUrl().startsWith(getInjectedUrl().toString())) {
            Thread.sleep(2000);
            logOut();
            navigateTo();
        }

        Thread.sleep(2000);

        if (scopes.length > 0) {
            StringBuilder scopesValue = new StringBuilder();

            for (String scope : scopes) {
                if (scopesValue.length() != 0) {
                    scopesValue.append(" ");
                }
                scopesValue.append(scope);
            }

            URLUtils.navigateToUri(this.driver.getCurrentUrl() + " " + scopesValue, true);
        }

        this.loginPage.form().login(username, password);

        // simple check if we are at the consent page, if so just click 'Yes'
        if (this.consentPage.isCurrent()) {
            consentPage.confirm();
        }

        pause(WAIT_AFTER_OPERATION);
    }

    public boolean wasDenied() {
        return this.driver.findElement(By.id("output")).getText().contains("You can not access");
    }

    public void viewAlbum(String name) throws InterruptedException {
        this.driver.findElement(By.xpath("//a[text() = '" + name + "']")).click();
        waitForPageToLoad();
        driver.navigate().refresh(); // This is sometimes necessary for loading the new policy settings
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
