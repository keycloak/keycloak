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
package org.keycloak.testsuite.actions;

import java.util.List;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.models.AccountRoles;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.pages.ErrorPage;

/**
 *
 * @author Stan Silvert
 */
public class AppInitiatedActionRoleTest extends AbstractAppInitiatedActionTest {
    
    @Page
    protected ErrorPage errorPage;
    
    public AppInitiatedActionRoleTest() {
        super("update_profile");
    }
    
    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        List<RoleRepresentation> roleList = testRealm.getRoles().getClient().get("test-app");
        
        RoleRepresentation manageAccountRole = null;
        for (RoleRepresentation role : roleList) {
            if (role.getName().equals(AccountRoles.MANAGE_ACCOUNT)) {
                manageAccountRole = role;
                break;
            }
        }
        
        roleList.remove(manageAccountRole);
    }
    
    @Test
    public void roleNotSetTest() {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        doAIA();
        errorPage.assertCurrent();
    }
}
