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
import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.net.URL;

/**
 * Created by fkiss.
 */
public class AngularCorsProductTestApp extends AbstractPageWithInjectedUrl {

    public static final String DEPLOYMENT_NAME = "angular-cors-product";
    public static final String CLIENT_ID = "integration-arquillian-test-apps-cors-angular-product";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Override
    public URL getInjectedUrl() {
        return url;
    }

    @FindByJQuery("button:contains('Reload')")
    private WebElement reloadDataButton;

    @FindByJQuery("button:contains('load Roles')")
    private WebElement loadRolesButton;

    @FindByJQuery("button:contains('Add Role')")
    private WebElement addRoleButton;

    @FindByJQuery("button:contains('Delete Role')")
    private WebElement deleteRoleButton;

    @FindByJQuery("button:contains('load available social providers')")
    private WebElement loadAvailableSocialProvidersButton;

    @FindByJQuery("button:contains('Load public realm info')")
    private WebElement loadPublicRealmInfoButton;

    @FindByJQuery("button:contains('Load version')")
    private WebElement loadVersionButton;

    @FindBy(id = "output")
    private WebElement outputArea;
    @FindBy(id = "headers")
    private WebElement headers;

    public void reloadData() {
        WaitUtils.waitUntilElement(reloadDataButton).is().clickable();
        reloadDataButton.click();
    }

    public void loadRoles() {
        WaitUtils.waitUntilElement(loadRolesButton).is().clickable();
        loadRolesButton.click();
    }

    public void addRole() {
        WaitUtils.waitUntilElement(addRoleButton).is().clickable();
        addRoleButton.click();
    }

    public void deleteRole() {
        WaitUtils.waitUntilElement(deleteRoleButton).is().clickable();
        deleteRoleButton.click();
    }

    public void loadAvailableSocialProviders() {
        WaitUtils.waitUntilElement(loadAvailableSocialProvidersButton).is().clickable();
        loadAvailableSocialProvidersButton.click();
    }

    public void loadPublicRealmInfo() {
        WaitUtils.waitUntilElement(loadPublicRealmInfoButton).is().clickable();
        loadPublicRealmInfoButton.click();
    }

    public void loadVersion() {
        WaitUtils.waitUntilElement(loadVersionButton).is().clickable();
        loadVersionButton.click();
    }

    public WebElement getOutput() {
        WaitUtils.waitUntilElement(outputArea).is().visible();
        return outputArea;
    }

    public WebElement getHeaders() {
        WaitUtils.waitUntilElement(headers).is().visible();
        return headers;
    }


}
