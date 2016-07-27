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
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.net.URL;

import static org.keycloak.testsuite.util.WaitUtils.pause;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PhotozClientAuthzTestApp extends AbstractPageWithInjectedUrl {

    public static final String DEPLOYMENT_NAME = "photoz-html5-client";
    public static final int WAIT_AFTER_OPERATION = 2000;

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Page
    protected OIDCLogin loginPage;

    public void createAlbum(String name) {
        this.driver.findElement(By.id("create-album")).click();
        Form.setInputValue(this.driver.findElement(By.id("album.name")), name);
        pause(200); // We need to wait a bit for the form to "accept" the input (otherwise it registers the input as empty)
        this.driver.findElement(By.id("save-album")).click();
        pause(WAIT_AFTER_OPERATION);
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
        this.driver.navigate().to(this.getInjectedUrl().toString() + "/#/admin/album");
        pause(WAIT_AFTER_OPERATION);
    }

    public void logOut() {
        navigateTo();
        By by = By.xpath("//a[text() = 'Sign Out']");
        this.driver.findElement(by).click();
        pause(WAIT_AFTER_OPERATION);
    }

    public void login(String username, String password) {
        navigateTo();

        if (this.driver.getCurrentUrl().startsWith(getInjectedUrl().toString())) {
            logOut();
        }

        this.loginPage.form().login(username, password);
    }

    public boolean wasDenied() {
        return this.driver.findElement(By.id("output")).getText().contains("You can not access");
    }
}
