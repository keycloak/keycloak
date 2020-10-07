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

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.AggregatePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.TimePolicyRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testsuite.console.page.fragment.ModalDialog;
import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.performOperationWithPageReload;
import static org.openqa.selenium.By.tagName;

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

    @Page
    private ClientPolicy clientPolicy;

    @Page
    private GroupPolicy groupPolicy;

    @Page
    private ModalDialog modalDialog;

    public PoliciesTable policies() {
        return table;
    }

    public <P extends PolicyTypeUI> P create(AbstractPolicyRepresentation expected, boolean save) {
        String type = expected.getType();

        performOperationWithPageReload(() -> createSelect.selectByValue(type));

        if ("role".equals(type)) {
            rolePolicy.form().populate((RolePolicyRepresentation) expected, save);
            return (P) rolePolicy;
        } else if ("user".equals(type)) {
            userPolicy.form().populate((UserPolicyRepresentation) expected, save);
            return (P) userPolicy;
        } else if ("aggregate".equals(type)) {
            aggregatePolicy.form().populate((AggregatePolicyRepresentation) expected, save);
            return (P) aggregatePolicy;
        } else if ("js".equals(type)) {
            jsPolicy.form().populate((JSPolicyRepresentation) expected, save);
            return (P) jsPolicy;
        } else if ("time".equals(type)) {
            timePolicy.form().populate((TimePolicyRepresentation) expected, save);
            return (P) timePolicy;
        } else if ("client".equals(type)) {
            clientPolicy.form().populate((ClientPolicyRepresentation) expected, save);
            return (P) clientPolicy;
        } else if ("group".equals(type)) {
            groupPolicy.form().populate((GroupPolicyRepresentation) expected, save);
            return (P) groupPolicy;
        }

        return null;
    }

    public <P extends PolicyTypeUI> P create(AbstractPolicyRepresentation expected) {
        return create(expected, true);
    }

    public void update(String name, AbstractPolicyRepresentation representation) {
        for (WebElement row : policies().rows()) {
            PolicyRepresentation actual = policies().toRepresentation(row);
            if (actual.getName().equalsIgnoreCase(name)) {
                clickLink(row.findElements(tagName("a")).get(0));
                String type = representation.getType();

                if ("role".equals(type)) {
                    rolePolicy.form().populate((RolePolicyRepresentation) representation, true);
                } else if ("user".equals(type)) {
                    userPolicy.form().populate((UserPolicyRepresentation) representation, true);
                } else if ("aggregate".equals(type)) {
                    aggregatePolicy.form().populate((AggregatePolicyRepresentation) representation, true);
                } else if ("js".equals(type)) {
                    jsPolicy.form().populate((JSPolicyRepresentation) representation, true);
                } else if ("time".equals(type)) {
                    timePolicy.form().populate((TimePolicyRepresentation) representation, true);
                } else if ("client".equals(type)) {
                    clientPolicy.form().populate((ClientPolicyRepresentation) representation, true);
                } else if ("group".equals(type)) {
                    groupPolicy.form().populate((GroupPolicyRepresentation) representation, true);
                }

                return;
            }
        }
    }

    public <P extends PolicyTypeUI> P name(String name) {
        for (WebElement row : policies().rows()) {
            PolicyRepresentation actual = policies().toRepresentation(row);
            if (actual.getName().equalsIgnoreCase(name)) {
                clickLink(row.findElements(tagName("a")).get(0));
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
                } else if ("client".equals(type)) {
                    return (P) clientPolicy;
                } else if ("group".equals(type)) {
                    return (P) groupPolicy;
                }
            }
        }
        return null;
    }

    public void delete(String name) {
        for (WebElement row : policies().rows()) {
            PolicyRepresentation actual = policies().toRepresentation(row);
            if (actual.getName().equalsIgnoreCase(name)) {
                clickLink(row.findElements(tagName("a")).get(0));

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
                } else if ("client".equals(type)) {
                    clientPolicy.form().delete();
                } else if ("group".equals(type)) {
                    groupPolicy.form().delete();
                }

                return;
            }
        }
    }

    public void deleteFromList(String name) {
        for (WebElement row : policies().rows()) {
            PolicyRepresentation actual = policies().toRepresentation(row);
            if (actual.getName().equalsIgnoreCase(name)) {
                row.findElements(tagName("td")).get(4).click();
                modalDialog.confirmDeletion();
                return;
            }
        }
    }
}