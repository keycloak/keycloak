/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.adapter.servlet;

import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.testsuite.arquillian.annotation.UseServletFilter;

@UseServletFilter(filterName = "oidc-filter", filterClass = "org.keycloak.adapters.servlet.KeycloakOIDCFilter",
        filterDependency = "org.keycloak:keycloak-servlet-filter-adapter", skipPattern = "/error.html")
public abstract class AbstractDemoFilterServletAdapterTest extends AbstractDemoServletsAdapterTest {


    @Test
    @Override
    @Ignore
    public void testNullBearerTokenCustomErrorPage() {
        //can't test because of the way filter works
    }
    
    @Test
    @Override
    @Ignore
    public void testAuthenticated() {
        //Don't need to test this because HttpServletRequest.authenticate doesn't make sense with filter implementation
    }

    @Test
    @Override
    @Ignore
    public void testAuthenticatedWithCustomSessionConfig() {

    }

    @Test
    @Override
    @Ignore
    public void testOIDCParamsForwarding() {

    }

    @Test
    @Override
    @Ignore
    public void testOIDCUiLocalesParamForwarding() {

    }
    
    @Test
    @Override
    @Ignore
    public void testTokenInCookieSSO() {
        
    }
    
    @Test
    @Override
    @Ignore
    public void testInvalidTokenCookie() {
        
    }
    
    @Test
    @Override
    @Ignore
    public void testTokenInCookieRefresh() {
        
    }
}
