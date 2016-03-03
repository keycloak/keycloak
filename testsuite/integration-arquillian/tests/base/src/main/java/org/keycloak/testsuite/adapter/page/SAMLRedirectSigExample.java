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

import java.net.URL;

/**
 * @author mhajas
 */
public class SAMLRedirectSigExample extends AbstractPageWithInjectedUrl {
    public static final String DEPLOYMENT_NAME = "saml-redirect-signatures";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @FindBy(tagName = "a")
    WebElement logoutButton;

    @Override
    public URL getInjectedUrl() {
        //EAP6 URL fix
        URL fixedUrl = createInjectedURL("employee-sig");
        return fixedUrl != null ? fixedUrl : url;
    }

    public void logout() {
        logoutButton.click();
    }
}