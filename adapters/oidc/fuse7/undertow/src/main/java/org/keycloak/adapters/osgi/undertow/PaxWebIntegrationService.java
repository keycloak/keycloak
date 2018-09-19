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

package org.keycloak.adapters.osgi.undertow;

import org.keycloak.adapters.osgi.PaxWebSecurityConstraintMapping;
import org.jboss.logging.Logger;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.util.List;

/**
 * Integration with pax-web, which allows to inject custom security constraint for securing resources by Keycloak.
 *
 * <p>It assumes that pax-web {@link WebContainer} is used as implementation of OSGI {@link org.osgi.service.http.HttpService}, which
 * is true in karaf/fuse environment</p>
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PaxWebIntegrationService {

    protected static final Logger log = Logger.getLogger(PaxWebIntegrationService.class);

    private BundleContext bundleContext;
    private List<PaxWebSecurityConstraintMapping> constraintMappings;

    private ServiceTracker webContainerTracker;
    private HttpContext httpContext;

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public List<PaxWebSecurityConstraintMapping> getConstraintMappings() {
        return constraintMappings;
    }

    public void setConstraintMappings(List<PaxWebSecurityConstraintMapping> constraintMappings) {
        this.constraintMappings = constraintMappings;
    }

    protected ServiceTracker getWebContainerTracker() {
        return webContainerTracker;
    }

    protected HttpContext getHttpContext() {
        return httpContext;
    }

    public void setHttpContext(HttpContext httpContext) {
        this.httpContext = httpContext;
    }

    public void start() {
        ServiceTrackerCustomizer trackerCustomizer = new ServiceTrackerCustomizer() {

            @Override
            public Object addingService(ServiceReference reference) {
                return addingWebContainerCallback(reference);
            }

            @Override
            public void modifiedService(ServiceReference reference, Object service) {
            }

            @Override
            public void removedService(ServiceReference reference, Object service) {
                removingWebContainerCallback(reference);
            }
        };

        webContainerTracker = new ServiceTracker(bundleContext, WebContainer.class.getName(), trackerCustomizer);
        webContainerTracker.open();
    }

    public void stop() {
        webContainerTracker.remove(webContainerTracker.getServiceReference());
    }

    protected WebContainer addingWebContainerCallback(ServiceReference webContainerServiceReference) {
        WebContainer service = (WebContainer) bundleContext.getService(webContainerServiceReference);
        if (httpContext == null) {
            httpContext = service.createDefaultHttpContext();
        }

        if (constraintMappings == null) {
            throw new IllegalStateException("constraintMappings was null!");
        }
        for (PaxWebSecurityConstraintMapping constraintMapping : constraintMappings) {
            addConstraintMapping(service, constraintMapping);
        }

        service.registerLoginConfig("KEYCLOAK", "specified-in-keycloak-json", null, null, httpContext);

        return service;
    }

    protected void addConstraintMapping(WebContainer service, PaxWebSecurityConstraintMapping cm) {
        log.debug("Adding security constraint name=" + cm.getConstraintName() + ", url=" + cm.getUrl() + ", dataConstraint=" + cm.getDataConstraint() + ", canAuthenticate="
                + cm.isAuthentication() + ", roles=" + cm.getRoles());
        service.registerConstraintMapping(
          cm.getConstraintName(),
          cm.getMapping(),
          cm.getUrl(),
          cm.getDataConstraint(),
          cm.isAuthentication(),
          cm.getRoles(),
          httpContext
        );
    }

    protected void removingWebContainerCallback(ServiceReference serviceReference) {
        WebContainer service = (WebContainer)bundleContext.getService(serviceReference);
        if (service != null) {
            service.unregisterLoginConfig(httpContext);
            service.unregisterConstraintMapping(httpContext);
        }
    }
}