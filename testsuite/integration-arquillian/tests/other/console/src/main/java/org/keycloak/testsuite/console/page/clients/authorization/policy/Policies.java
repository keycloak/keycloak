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
package org.keycloak.testsuite.console.page.clients.authorization.policy;

import static org.openqa.selenium.By.tagName;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.AggregatePolicyRepresentation;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.TimePolicyRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class Policies extends Form {

    @FindBy(css = "table[class*='table']")
    private PoliciesTable table;

    @FindBy(id = "create-policy")
    private Select createSelect;

    @Page
    private RolePolicy rolePolicy;

    @Page
    private UserPolicy userPolicy;

    @Page
    private AggregatePolicy aggregatePolicy;

    @Page
    private JSPolicy jsPolicy;

    @Page
    private TimePolicy timePolicy;

    public PoliciesTable policies() {
        return table;
    }

    public <P extends PolicyTypeUI> P create(AbstractPolicyRepresentation expected) {
        String type = expected.getType();

        createSelect.selectByValue(type);

        if ("role".equals(type)) {
            rolePolicy.form().populate((RolePolicyRepresentation) expected);
            rolePolicy.form().save();
            return (P) rolePolicy;
        } else if ("user".equals(type)) {
            userPolicy.form().populate((UserPolicyRepresentation) expected);
            userPolicy.form().save();
            return (P) userPolicy;
        } else if ("aggregate".equals(type)) {
            aggregatePolicy.form().populate((AggregatePolicyRepresentation) expected);
            aggregatePolicy.form().save();
            return (P) aggregatePolicy;
        } else if ("js".equals(type)) {
            jsPolicy.form().populate((JSPolicyRepresentation) expected);
            jsPolicy.form().save();
            return (P) jsPolicy;
        } else if ("time".equals(type)) {
            timePolicy.form().populate((TimePolicyRepresentation) expected);
            timePolicy.form().save();
            return (P) timePolicy;
        }

        return null;
    }

    public void update(String name, AbstractPolicyRepresentation representation) {
        for (WebElement row : policies().rows()) {
            PolicyRepresentation actual = policies().toRepresentation(row);
            if (actual.getName().equalsIgnoreCase(name)) {
                row.findElements(tagName("a")).get(0).click();
                WaitUtils.waitForPageToLoad(driver);
                String type = representation.getType();

                if ("role".equals(type)) {
                    rolePolicy.form().populate((RolePolicyRepresentation) representation);
                } else if ("user".equals(type)) {
                    userPolicy.form().populate((UserPolicyRepresentation) representation);
                } else if ("aggregate".equals(type)) {
                    aggregatePolicy.form().populate((AggregatePolicyRepresentation) representation);
                } else if ("js".equals(type)) {
                    jsPolicy.form().populate((JSPolicyRepresentation) representation);
                } else if ("time".equals(type)) {
                    timePolicy.form().populate((TimePolicyRepresentation) representation);
                }

                return;
            }
        }
    }

    public <P extends PolicyTypeUI> P name(String name) {
        for (WebElement row : policies().rows()) {
            PolicyRepresentation actual = policies().toRepresentation(row);
            if (actual.getName().equalsIgnoreCase(name)) {
                row.findElements(tagName("a")).get(0).click();
                WaitUtils.waitForPageToLoad(driver);
                String type = actual.getType();
                if ("role".equals(type)) {
                    return (P) rolePolicy;
                } else if ("user".equals(type)) {
                    return (P) userPolicy;
                } else if ("aggregate".equals(type)) {
                    return (P) aggregatePolicy;
                } else if ("js".equals(type)) {
                    return (P) jsPolicy;
                } else if ("time".equals(type)) {
                    return (P) timePolicy;
                }
            }
        }
        return null;
    }

    public void delete(String name) {
        for (WebElement row : policies().rows()) {
            PolicyRepresentation actual = policies().toRepresentation(row);
            if (actual.getName().equalsIgnoreCase(name)) {
                row.findElements(tagName("a")).get(0).click();
                WaitUtils.waitForPageToLoad(driver);

                String type = actual.getType();

                if ("role".equals(type)) {
                    rolePolicy.form().delete();
                } else if ("user".equals(type)) {
                    userPolicy.form().delete();
                } else if ("aggregate".equals(type)) {
                    aggregatePolicy.form().delete();
                } else if ("js".equals(type)) {
                    jsPolicy.form().delete();
                } else if ("time".equals(type)) {
                    timePolicy.form().delete();
                }

                return;
            }
        }
    }
}