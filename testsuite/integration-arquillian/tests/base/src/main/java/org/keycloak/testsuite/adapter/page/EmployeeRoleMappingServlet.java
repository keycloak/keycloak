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

package org.keycloak.testsuite.adapter.page;

import java.net.URL;

import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.auth.page.login.SAMLPostLogin;
import org.keycloak.testsuite.util.WaitUtils;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.openqa.selenium.By;

/**
 * A {@code Page} for the {@code EmployeeRoleMapping} application.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class EmployeeRoleMappingServlet extends SAMLServlet {

    public static final String DEPLOYMENT_NAME = "employee-role-mapping";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    private SAMLPostLogin loginPage;
    private UserRepresentation user;

    @Override
    public URL getInjectedUrl() {
        return url;
    }

    /**
     * For scenarios in which the access to every method in the servlet is required to be authenticated (for example, when
     * running the servlet filter tests) we need to setup the info required to perform the login before calling the servlet
     * methods used by the test (such as {@code setCheckRoles} or {@code (un)checkRoles}).
     *
     * @param page the login page to be used to authenticate an user.
     * @param user the user being authenticated.
     */
    public void setupLoginInfo(final SAMLPostLogin page, final UserRepresentation user) {
        this.loginPage = page;
        this.user = user;
    }

    /**
     * Clears the login info (login page and user) so that no authentication is performed before the calling the servlet
     * methods that don't require authentication.
     */
    public void clearLoginInfo() {
        this.loginPage = null;
        this.user = null;
    }

    @Override
    public void setRolesToCheck(String roles) {
        if (this.loginPage != null) {
            // authenticates user before calling setCheckRoles on the servlet - required in filter tests.
            driver.navigate().to(getUriBuilder().clone().path("setCheckRoles").queryParam("roles", roles).build().toASCIIString());
            loginPage.form().login(user);
            WaitUtils.waitUntilElement(By.tagName("body")).text().contains("These roles will be checked:");
            this.logout();
        } else {
            super.setRolesToCheck(roles);
        }
    }

    @Override
    public void checkRolesEndPoint(boolean value) {
        if (this.loginPage != null) {
            // authenticates user before calling setCheckRoles on the servlet - required in filter tests.
            driver.navigate().to(getUriBuilder().clone().path((value ? "" : "un") + "checkRoles").build().toASCIIString());
            loginPage.form().login(user);
            WaitUtils.waitUntilElement(By.tagName("body")).text().contains("Roles will " + (value ? "" : "not ") +  "be checked");
            this.logout();
        } else {
            super.checkRolesEndPoint(value);
        }
    }
}
