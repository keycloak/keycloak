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

package org.keycloak.adapters.osgi;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.Servlet;

import org.jboss.logging.Logger;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Service, which allows to remove previously registered servlets in karaf/fuse environment. It assumes that particular servlet was previously
 * registered as service in OSGI container under {@link javax.servlet.Servlet} interface.
 *
 * <p>The point is to register automatically registered builtin servlet endpoints (like "/cxf" for instance) to allow secure them
 * by Keycloak and re-register them again</p>
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ServletReregistrationService {

    protected static final Logger log = Logger.getLogger(ServletReregistrationService.class);

    private static final List<String> FILTERED_PROPERTIES = Arrays.asList("objectClass", "service.id");

    private BundleContext bundleContext;
    private ServiceReference servletReference;
    private ServiceTracker webContainerTracker;

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public ServiceReference getServletReference() {
        return servletReference;
    }

    public void setServletReference(ServiceReference servletReference) {
        this.servletReference = servletReference;
    }

    protected ServiceTracker getWebContainerTracker() {
        return webContainerTracker;
    }

    public void start() {
        if (servletReference == null) {
            throw new IllegalStateException("No servlet reference provided");
        }

        final Servlet servlet = (Servlet) bundleContext.getService(servletReference);
        WebContainer externalWebContainer = findExternalWebContainer();
        if (externalWebContainer == null) {
            return;
        }

        // Unregister servlet from external container now
        try {
            externalWebContainer.unregisterServlet(servlet);
            log.debug("Original servlet with alias " + getAlias() + " unregistered successfully from external web container.");
        } catch (IllegalStateException e) {
            log.warn("Can't unregister servlet due to: " + e.getMessage());
        }

        ServiceTrackerCustomizer trackerCustomizer = new ServiceTrackerCustomizer() {

            @Override
            public Object addingService(ServiceReference webContainerServiceReference) {
                WebContainer ourWebContainer = (WebContainer) bundleContext.getService(webContainerServiceReference);
                registerServlet(ourWebContainer, servlet);
                log.debugv("Servlet with alias " + getAlias() + " registered to secured web container");
                return ourWebContainer;
            }

            @Override
            public void modifiedService(ServiceReference reference, Object service) {
            }

            @Override
            public void removedService(ServiceReference webContainerServiceReference, Object service) {
                WebContainer ourWebContainer = (WebContainer) bundleContext.getService(webContainerServiceReference);
                String alias = getAlias();
                ourWebContainer.unregister(alias);
                log.debug("Servlet with alias " + getAlias() + " unregistered from secured web container");
            }
        };

        webContainerTracker = new ServiceTracker(bundleContext, WebContainer.class.getName(), trackerCustomizer);
        webContainerTracker.open();
    }

    public void stop() {
        // Stop tracking our container now and removing reference. This should unregister servlet from our container via trackerCustomizer.removedService (if it's not already unregistered)
        webContainerTracker.remove(webContainerTracker.getServiceReference());

        // Re-register servlet back to original context
        WebContainer externalWebContainer = findExternalWebContainer();
        Servlet servlet = (Servlet) bundleContext.getService(servletReference);
        registerServlet(externalWebContainer, servlet);
        log.debug("Servlet with alias " + getAlias() + " registered back to external web container");
    }

    private String getAlias() {
        return (String) servletReference.getProperty("alias");
    }

    protected void registerServlet(WebContainer webContainer, Servlet servlet) {
        try {
            Hashtable<String, Object> servletInitParams = new Hashtable<String, Object>();
            String[] propNames = servletReference.getPropertyKeys();
            for (String propName : propNames) {
                if (!FILTERED_PROPERTIES.contains(propName)) {
                    servletInitParams.put(propName, servletReference.getProperty(propName));
                }
            }

            // Try to register servlet in given web container now
            HttpContext httpContext = webContainer.createDefaultHttpContext();
            String alias = (String) servletReference.getProperty("alias");
            webContainer.registerServlet(alias, servlet, servletInitParams, httpContext);
        } catch (Exception e) {
            log.error("Can't register servlet in web container", e);
        }
    }

    /**
     * Find web container in the bundle, where was servlet originally registered
     *
     * @return web container or null
     */
    protected WebContainer findExternalWebContainer() {
        BundleContext servletBundleContext = servletReference.getBundle().getBundleContext();
        ServiceReference webContainerReference = servletBundleContext.getServiceReference(WebContainer.class.getName());
        if (webContainerReference == null) {
            log.warn("Not found webContainer reference for bundle " + servletBundleContext);
            return null;
        } else {
            return (WebContainer) servletBundleContext.getService(webContainerReference);
        }
    }

}
