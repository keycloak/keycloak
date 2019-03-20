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
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import org.keycloak.testsuite.util.URLUtils;

public class CustomerCookiePortalRoot extends AbstractPageWithInjectedUrl {

    public static final String DEPLOYMENT_NAME = "customer-cookie-portal-root";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Override
    public URL getInjectedUrl() {
        return url;
    }
    
    public String logoutURL() {
        return  getInjectedUrl() + "/logout";
    }

    public void navigateTo(String relative) {
        URLUtils.navigateToUri(getInjectedUrl() + relative);
    }

    @Override
    public String toString() {
        return getInjectedUrl().toString();
    }
}
