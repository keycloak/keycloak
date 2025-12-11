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
package org.keycloak.testsuite.auth.page.login;

import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.common.util.CollectionUtil;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.testsuite.util.DroneUtils;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.clickLink;

import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class OAuthGrant extends RequiredActions {
    @FindBy(css = "button[name=\"accept\"]")
    private WebElement acceptButton;

    @FindBy(css = "button[name=\"cancel\"]")
    private WebElement cancelButton;

    @FindBy(xpath = "//div[@id='kc-oauth']/ul/li/span")
    private List<WebElement> scopesToApprove;

    @Override
    public String getActionId() {
        return AuthenticatedClientSessionModel.Action.OAUTH_GRANT.name();
    }

    public void accept() {
        clickLink(acceptButton);
    }

    @Override
    public void cancel() {
        clickLink(cancelButton);
    }

    public boolean isCurrent(WebDriver driver1) {
        DroneUtils.addWebDriver(driver1);
        boolean ret = super.isCurrent();
        DroneUtils.removeWebDriver();
        return ret;
    }

    public void assertClientScopes(List<String> expectedScopes) {
        List<String> actualScopes = scopesToApprove.stream().map(WebElement::getText).collect(Collectors.toList());
        assertTrue("Expected and actual Client Scopes to approve don't match",
                CollectionUtil.collectionEquals(expectedScopes, actualScopes)); // order of scopes doesn't matter
    }
}
