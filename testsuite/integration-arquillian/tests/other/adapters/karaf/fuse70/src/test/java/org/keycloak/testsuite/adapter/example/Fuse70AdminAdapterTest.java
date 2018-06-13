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

package org.keycloak.testsuite.adapter.example;

import org.keycloak.testsuite.adapter.page.Hawtio2Page;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.util.WaitUtils;
import java.util.Arrays;
import java.util.List;
import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.openqa.selenium.By;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlDoesntStartWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

@AppServerContainer("app-server-fuse70")
public class Fuse70AdminAdapterTest extends AbstractFuseAdminAdapterTest {

    @Page
    protected Hawtio2Page hawtioPage;

    @Test
    @Override
    public void hawtioLoginTest() throws Exception {
        hawtioPage.navigateTo();
        WaitUtils.waitForPageToLoad();

        testRealmLoginPage.form().login("user", "invalid-password");
        assertCurrentUrlDoesntStartWith(hawtioPage);

        testRealmLoginPage.form().login("invalid-user", "password");
        assertCurrentUrlDoesntStartWith(hawtioPage);

        testRealmLoginPage.form().login("root", "password");
        assertCurrentUrlStartsWith(hawtioPage.toString(), hawtioPage.getDriver());
        WaitUtils.waitForPageToLoad();
        WaitUtils.waitUntilElement(By.linkText("Camel"));
        hawtioPage.logout();
        WaitUtils.waitForPageToLoad();

        assertCurrentUrlStartsWith(testRealmLoginPage);

        hawtioPage.navigateTo();
        WaitUtils.waitForPageToLoad();

        testRealmLoginPage.form().login("mary", "password");
        assertCurrentUrlStartsWith(hawtioPage.toString(), hawtioPage.getDriver());
        WaitUtils.waitForPageToLoad();
        WaitUtils.waitUntilElementIsNotPresent(By.linkText("Camel"));
    }

    @Test
    @Override
    public void sshLoginTest() throws Exception {
        assertCommand("mary", "password", "shell:date", Result.NOT_FOUND);
        assertCommand("john", "password", "shell:info", Result.NOT_FOUND);
        assertCommand("john", "password", "shell:date", Result.OK);
        assertRoles("root", 
          "ssh",
          "jmxAdmin",
          "admin",
          "manager",
          "viewer",
          "Administrator",
          "Auditor",
          "Deployer",
          "Maintainer",
          "Operator",
          "SuperUser"
        );
    }

    private void assertRoles(String username, String... expectedRoles) throws Exception {
        final String commandOutput = getCommandOutput(username, "password", "jaas:whoami -r --no-format");
        final List<String> parsedOutput = Arrays.asList(commandOutput.split("\\n+"));
        assertThat(parsedOutput, Matchers.containsInAnyOrder(expectedRoles));
    }
}
