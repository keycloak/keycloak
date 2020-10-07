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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriBuilder;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import org.keycloak.testsuite.util.WaitUtils;

import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

import org.openqa.selenium.By;

/**
 * @author mhajas
 */
public abstract class SAMLServlet extends AbstractPageWithInjectedUrl {

    public void logout() {
        driver.navigate().to(getUriBuilder().clone().queryParam("GLO", "true").build().toASCIIString());
        waitForPageToLoad();
    }

    public void checkRoles(boolean check) {
        if (check) {
            getUriBuilder().queryParam("checkRoles", true);
        } else {
            getUriBuilder().replaceQueryParam("checkRoles");
        }
    }

    public void checkRolesEndPoint(boolean value) {
        driver.navigate().to(getUriBuilder().clone().path((value ? "" : "un") + "checkRoles").build().toASCIIString());
        waitForPageToLoad();
    }

    public void setRolesToCheck(String roles) {
        UriBuilder uriBuilder = getUriBuilder().clone();
        String toASCIIString = uriBuilder.path("setCheckRoles").queryParam("roles", roles).build().toASCIIString();
        driver.navigate().to(toASCIIString);
        waitForPageToLoad();
        WaitUtils.waitUntilElement(By.tagName("body")).text().contains("These roles will be checked:");
    }

    public List<String> rolesList() {
        String rolesPattern = getFromPageByPattern("Roles");
        if (rolesPattern != null) {
            return Arrays.stream(rolesPattern.split(",")).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    public String getFromPageByPattern(String text) {
        Pattern p = Pattern.compile(text + ": (.*)");
        Matcher m = p.matcher(driver.getPageSource());
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
}
