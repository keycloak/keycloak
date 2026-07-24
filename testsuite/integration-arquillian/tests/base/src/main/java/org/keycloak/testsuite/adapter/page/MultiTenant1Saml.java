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

package org.keycloak.testsuite.adapter.page;

import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;

import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 * @author rmartinc
 */
public class MultiTenant1Saml extends SAMLServlet {
    public static final String DEPLOYMENT_NAME = "multi-tenant-saml";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Override
    public URL getInjectedUrl() {
        try {
            return new URL(url + "?realm=tenant1");
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }
    
    @Override
    public void logout() {
        driver.navigate().to(getUriBuilder().clone().queryParam("GLO", "true").queryParam("realm", "tenant1").build().toASCIIString());
        getUriBuilder().replaceQueryParam("GLO");
        pause(300);
    }
}
