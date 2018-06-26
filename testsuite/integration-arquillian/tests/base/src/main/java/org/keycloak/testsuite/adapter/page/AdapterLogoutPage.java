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
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;

import java.net.URL;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * @author mhajas
 */
public class AdapterLogoutPage extends AbstractPageWithInjectedUrl {

    public static final String DEPLOYMENT_NAME = "logout";

    private static final String WEB_XML =
        "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\" version=\"3.0\">"
      + "   <module-name>" + DEPLOYMENT_NAME + "</module-name>"
      + "</web-app>";

    private static final String LOGOUT_PAGE_HTML = "<html><body>Logged out</body></html>";

    public static final WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, AdapterLogoutPage.DEPLOYMENT_NAME + ".war")
          .addAsWebInfResource(new StringAsset(WEB_XML), "web.xml")
          .add(new StringAsset(LOGOUT_PAGE_HTML), "/index.html");
    }

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Override
    public URL getInjectedUrl() {
        return url;
    }

    @Override
    public boolean isCurrent() {
        return driver.getCurrentUrl().startsWith(url.toString());
    }
}
