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
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.util.URLUtils;

public class URLProvider extends URLResourceProvider {

    protected final Logger log = Logger.getLogger(this.getClass());

    public static final String BOUND_TO_ALL = "0.0.0.0";
    public static final String LOCALHOST_ADDRESS = "127.0.0.1";
    public static final String LOCALHOST_HOSTNAME = "localhost";

    private final boolean appServerSslRequired = Boolean.parseBoolean(System.getProperty("app.server.ssl.required"));

    @Inject
    Instance<SuiteContext> suiteContext;
    @Inject
    Instance<TestContext> testContext;

    private static final Set<String> fixedUrls = new HashSet<>();

    @Override
    public Object doLookup(ArquillianResource resource, Annotation... qualifiers) {
        URL url = (URL) super.doLookup(resource, qualifiers);

        if (url == null) {
            String port = appServerSslRequired ? 
                                System.getProperty("app.server.https.port", "8643") : 
                                System.getProperty("app.server.http.port", "8280");
            String protocol = appServerSslRequired ? "https" : "http";

            try {
                for (Annotation a : qualifiers) {
                    if (OperateOnDeployment.class.isAssignableFrom(a.annotationType())) {
                        return new URL(protocol + "://localhost:" + port + "/" + ((OperateOnDeployment) a).value() + "/");
                    }
                }
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        }
        
        // fix injected URL
        if (url != null) {
            try {
                url = fixLocalhost(url);
                url = fixBoundToAll(url);
                if (appServerSslRequired) {
                    url = fixSsl(url);
                }
            } catch (MalformedURLException ex) {
                log.log(Level.FATAL, null, ex);
            }

            if (!fixedUrls.contains(url.toString())) {
                fixedUrls.add(url.toString());
                log.debug("Fixed injected @ArquillianResource URL to: " + url);
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

        return url;
    }

    public URL fixBoundToAll(URL url) throws MalformedURLException {
        URL fixedUrl = url;
        if (url.getHost().contains(BOUND_TO_ALL)) {
            fixedUrl = new URL(fixedUrl.toExternalForm().replace(BOUND_TO_ALL, LOCALHOST_HOSTNAME));
        }
        return fixedUrl;
    }

    public URL fixLocalhost(URL url) throws MalformedURLException {
        URL fixedUrl = url;
        if (url.getHost().contains(LOCALHOST_ADDRESS)) {
            fixedUrl = new URL(fixedUrl.toExternalForm().replace(LOCALHOST_ADDRESS, LOCALHOST_HOSTNAME));
        }
        return fixedUrl;
    }

    public URL fixSsl(URL url) throws MalformedURLException {
        URL fixedUrl = url;
        String urlString = fixedUrl.toExternalForm().replace("http", "https").replace(System.getProperty("app.server.http.port", "8280"), System.getProperty("app.server.https.port", "8643"));
        return new URL(urlString);
    }

}
