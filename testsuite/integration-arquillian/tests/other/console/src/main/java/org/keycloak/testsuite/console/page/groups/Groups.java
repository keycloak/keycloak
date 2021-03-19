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
package org.keycloak.testsuite.console.page.groups;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.keycloak.testsuite.console.page.fragment.DataTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author clementcur
 */
public class Groups extends AdminConsoleRealm {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/groups";
    }

    public static final String NEW_GROUP = "New";

    @FindBy(id = "group-table")
    private GroupsTable table;

    public GroupsTable table() {
        return table;
    }

    public class GroupsTable extends DataTable {

        @Drone
        private WebDriver driver;

        public void addGroup() {
            clickHeaderButton(NEW_GROUP);
        }
    }
    
}
