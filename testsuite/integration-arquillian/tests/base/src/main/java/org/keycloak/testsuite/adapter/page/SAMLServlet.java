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

import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;

import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 * @author mhajas
 */
public abstract class SAMLServlet extends AbstractPageWithInjectedUrl {

    public void logout() {
        driver.navigate().to(getUriBuilder().queryParam("GLO", "true").build().toASCIIString());
        getUriBuilder().replaceQueryParam("GLO", null);
        pause(300);
    }

    public void checkRoles(boolean check) {
        if (check) {
            getUriBuilder().queryParam("checkRoles", true);
        } else {
            getUriBuilder().replaceQueryParam("checkRoles", null);
        }
    }

    public void checkRolesEndPoint() {
        driver.navigate().to(getUriBuilder().build().toASCIIString() + "/checkRoles");
        pause(300);
    }
}
