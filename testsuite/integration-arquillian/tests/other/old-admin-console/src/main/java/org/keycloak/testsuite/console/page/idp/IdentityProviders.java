/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.console.page.idp;

import org.jboss.arquillian.graphene.fragment.Root;
import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.keycloak.testsuite.console.page.fragment.DataTable;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import static org.keycloak.testsuite.util.UIUtils.isElementVisible;
import static org.keycloak.testsuite.util.UIUtils.performOperationWithPageReload;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class IdentityProviders extends AdminConsoleRealm {
    @FindBy(xpath = "//div[contains(@class,'blank-slate')]//select")
    private Select addProviderBlankSlateSelect;

    @FindBy(tagName = "table")
    private IdentityProvidersTable table;

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/identity-provider-settings";
    }

    public IdentityProvidersTable table() {
        return table;
    }

    public void addProvider(final String providerId) {
        Select idpSelect = table.isVisible() ? table.addProviderTableSelect : addProviderBlankSlateSelect;
        performOperationWithPageReload(() -> idpSelect.selectByValue(providerId));
    }

    public class IdentityProvidersTable extends DataTable {
        @Root
        private WebElement tableRoot;

        @FindBy(tagName = "select")
        private Select addProviderTableSelect;

        public boolean isVisible() {
            return isElementVisible(tableRoot);
        }

        public void clickProvider(final String alias) {
            clickRowByLinkText(alias);
        }
    }
}
