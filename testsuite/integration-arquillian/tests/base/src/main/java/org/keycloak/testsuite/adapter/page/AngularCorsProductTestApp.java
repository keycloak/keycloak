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

    public void reloadData() {
        reloadDataButton.click();
    }

    public void loadRoles() {
        loadRolesButton.click();
    }

    public void addRole() {
        addRoleButton.click();
    }

    public void deleteRole() {
        deleteRoleButton.click();
    }

    public void loadAvailableSocialProviders() {
        loadAvailableSocialProvidersButton.click();
    }

    public void loadPublicRealmInfo() {
        loadPublicRealmInfoButton.click();
    }

    public void loadVersion() {
        loadVersionButton.click();
    }

    public WebElement getOutput() {
        return outputArea;
    }


}