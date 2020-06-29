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

package org.keycloak.testsuite.arquillian.provider;

import org.apache.http.client.utils.URIBuilder;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.impl.enricher.resource.URLResourceProvider;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.arquillian.TestContext;
import org.keycloak.testsuite.arquillian.annotation.AppServerBrowserContext;
import org.keycloak.testsuite.arquillian.annotation.AppServerContext;
import org.keycloak.testsuite.arquillian.annotation.AuthServerBrowserContext;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContext;

import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.testsuite.util.URLUtils;

import static org.keycloak.testsuite.util.ServerURLs.APP_SERVER_HOST;
import static org.keycloak.testsuite.util.ServerURLs.APP_SERVER_PORT;
import static org.keycloak.testsuite.util.ServerURLs.APP_SERVER_SCHEME;

public class URLProvider extends URLResourceProvider {

    protected final Logger log = Logger.getLogger(this.getClass());

    @Inject
    Instance<SuiteContext> suiteContext;
    @Inject
    Instance<TestContext> testContext;

    @Override
    public Object doLookup(ArquillianResource resource, Annotation... qualifiers) {
        URL url = (URL) super.doLookup(resource, qualifiers);

        if (url == null) {
            String appServerContextRoot = ServerURLs.getAppServerContextRoot();
            try {
                for (Annotation a : qualifiers) {
                    if (OperateOnDeployment.class.isAssignableFrom(a.annotationType())) {
                        return new URL(appServerContextRoot + "/" + ((OperateOnDeployment) a).value() + "/");
                    }
                }
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        }

        // inject context roots if annotation present
        for (Annotation a : qualifiers) {
            if (AuthServerContext.class.isAssignableFrom(a.annotationType())) {
                return suiteContext.get().getAuthServerInfo().getContextRoot();
            }
            if (AppServerContext.class.isAssignableFrom(a.annotationType())) {
                //standalone
                ContainerInfo appServerInfo = testContext.get().getAppServerInfo();
                if (appServerInfo != null) return appServerInfo.getContextRoot();

                //cluster
                List<ContainerInfo> appServerBackendsInfo = testContext.get().getAppServerBackendsInfo();
                if (appServerBackendsInfo.isEmpty()) throw new IllegalStateException("Both testContext's appServerInfo and appServerBackendsInfo not set.");

                return appServerBackendsInfo.get(0).getContextRoot();
            }
            if (AuthServerBrowserContext.class.isAssignableFrom(a.annotationType())) {
                return suiteContext.get().getAuthServerInfo().getBrowserContextRoot();
            }
            if (AppServerBrowserContext.class.isAssignableFrom(a.annotationType())) {
                //standalone
                ContainerInfo appServerInfo = testContext.get().getAppServerInfo();
                if (appServerInfo != null) return appServerInfo.getBrowserContextRoot();

                //cluster
                List<ContainerInfo> appServerBackendsInfo = testContext.get().getAppServerBackendsInfo();
                if (appServerBackendsInfo.isEmpty()) throw new IllegalStateException("Both testContext's appServerInfo and appServerBackendsInfo not set.");

                return appServerBackendsInfo.get(0).getBrowserContextRoot();
            }
        }

        // fix injected URL
        if (url != null) {
                        try {
                url = new URIBuilder(url.toURI())
                        .setScheme(APP_SERVER_SCHEME)
                        .setHost(APP_SERVER_HOST)
                        .setPort(Integer.parseInt(APP_SERVER_PORT))
                        .build().toURL();
            } catch (URISyntaxException | MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        }

        return url;
    }
}
