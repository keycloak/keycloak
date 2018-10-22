/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.console.page.idp;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.keycloak.testsuite.model.Provider;
import org.keycloak.testsuite.model.SocialProvider;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import java.util.ArrayList;
import java.util.List;

import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.openqa.selenium.By.tagName;

/**
 *
 * @author Petr Mensik
 */
public class IdentityProviderSettings extends AdminConsoleRealm {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "identity-provider-settings";
    }

    @FindBy(tagName = "select")
    private Select newProviderSelect;

    @FindByJQuery("input[class*='form-control']:eq(1)")
    private WebElement providerKey;

    @FindByJQuery("input[class*='form-control']:eq(2)")
    private WebElement providerSecret;

    @FindBy(tagName = "tbody")
    private WebElement providersTable;

    public void addNewProvider(Provider provider) {
        newProviderSelect.selectByVisibleText(provider.providerName.getName());
        providerKey.sendKeys(provider.key);
        providerSecret.sendKeys(provider.secret);
        primaryButton.click();
    }

    public void editProvider(SocialProvider oldProvider, Provider newProvider) {
        Provider p = find(oldProvider);
        if (p == null) {
            throw new AssertionError("Provider should have been found");
        }
        log.info(p.providerName);
    }

    public Provider find(SocialProvider provider) {
        List<Provider> list = getAllRows();
        for (Provider p : list) {
            if (p.providerName == provider) {
                return p;
            }
        }
        return null;
    }

    private List<Provider> getAllRows() {
        List<Provider> rows = new ArrayList<>();
        for (WebElement rowElement : providersTable.findElements(tagName("tr"))) {
            Provider provider = new Provider();
            List<WebElement> tds = rowElement.findElements(tagName("td"));
            if (!(tds.isEmpty() || getTextFromElement(tds.get(0)).isEmpty())) {
                provider.providerName = SocialProvider.valueOf(getTextFromElement(tds.get(0)));
                provider.key = getTextFromElement(tds.get(1));
                provider.secret = getTextFromElement(tds.get(2));
                rows.add(provider);
            }
        }
        return rows;
    }
    
}
