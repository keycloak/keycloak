/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.console.page.clients.authorization.permission;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.testsuite.console.page.clients.authorization.policy.PolicyTypeUI;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.openqa.selenium.By.tagName;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class Permissions extends Form {

    @FindBy(css = "table[class*='table']")
    private PermissionsTable table;

    @FindBy(id = "create-permission")
    private Select createSelect;

    @Page
    private ResourcePermission resourcePermission;

    @Page
    private ScopePermission scopePermission;

    public PermissionsTable permissions() {
        return table;
    }

    public <P extends PolicyTypeUI> P create(AbstractPolicyRepresentation expected) {
        String type = expected.getType();

        createSelect.selectByValue(type);

        if ("resource".equals(type)) {
            resourcePermission.form().populate((ResourcePermissionRepresentation) expected);
            return (P) resourcePermission;
        } else if ("scope".equals(type)) {
            scopePermission.form().populate((ScopePermissionRepresentation) expected);
            return (P) scopePermission;
        }

        return null;
    }

    public void update(String name, AbstractPolicyRepresentation representation) {
        for (WebElement row : permissions().rows()) {
            PolicyRepresentation actual = permissions().toRepresentation(row);
            if (actual.getName().equalsIgnoreCase(name)) {
                clickLink(row.findElements(tagName("a")).get(0));
                WaitUtils.waitForPageToLoad();
                String type = representation.getType();

                if ("resource".equals(type)) {
                    resourcePermission.form().populate((ResourcePermissionRepresentation) representation);
                } else if ("scope".equals(type)) {
                    scopePermission.form().populate((ScopePermissionRepresentation) representation);
                }

                return;
            }
        }
    }

    public <P extends PolicyTypeUI> P name(String name) {
        for (WebElement row : permissions().rows()) {
            PolicyRepresentation actual = permissions().toRepresentation(row);
            if (actual.getName().equalsIgnoreCase(name)) {
                clickLink(row.findElements(tagName("a")).get(0));
                WaitUtils.waitForPageToLoad();
                String type = actual.getType();
                if ("resource".equals(type)) {
                    return (P) resourcePermission;
                } else if ("scope".equals(type)) {
                    return (P) scopePermission;
                }
            }
        }
        return null;
    }

    public void delete(String name) {
        for (WebElement row : permissions().rows()) {
            PolicyRepresentation actual = permissions().toRepresentation(row);
            if (actual.getName().equalsIgnoreCase(name)) {
                clickLink(row.findElements(tagName("a")).get(0));
                WaitUtils.waitForPageToLoad();

                String type = actual.getType();

                if ("resource".equals(type)) {
                    resourcePermission.form().delete();
                } else if ("scope".equals(type)) {
                    scopePermission.form().delete();
                }

                return;
            }
        }
    }

    public void deleteFromList(String name) {
        for (WebElement row : permissions().rows()) {
            PolicyRepresentation actual = permissions().toRepresentation(row);
            if (actual.getName().equalsIgnoreCase(name)) {
                row.findElements(tagName("td")).get(4).click();
                driver.findElement(By.xpath(".//button[text()='Delete']")).click();
                return;
            }
        }
    }
}