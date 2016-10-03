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

import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.jboss.logging.Logger;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.http.HttpContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import javax.servlet.Servlet;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

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

    private static final String CXF_SERVLET_PREFIX = "org.apache.cxf.servlet.";
    protected static final Logger log = Logger.getLogger(ServletReregistrationService.class);

    private static final List<String> FILTERED_PROPERTIES = Arrays.asList("objectClass", "service.id");

    private BundleContext bundleContext;
    private ServiceReference managedServiceReference;
    private ServiceTracker webContainerTracker;
    private String alias;

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }


    public void start() {
        if ( managedServiceReference == null) {
            return;
        }

        Dictionary properties = obtainProperties();
        alias = (String)getProp(properties, CXF_SERVLET_PREFIX + "context", "/cxf");
        if(alias == null){
            alias = "/cxf";
        }

        WebContainer externalWebContainer = findExternalWebContainer();
        if (externalWebContainer == null) {
            return;
        }

        // Unregister servlet from external container now
        try {
            externalWebContainer.unregister(alias);
            log.debug("Original servlet with alias " + getAlias() + " unregistered successfully from external web container.");
        } catch (IllegalStateException e) {
            log.warn("Can't unregister servlet due to: " + e.getMessage());
        }

        final Dictionary finalProperties = properties;
        ServiceTrackerCustomizer trackerCustomizer = new ServiceTrackerCustomizer() {

            @Override
            public Object addingService(ServiceReference webContainerServiceReference) {
                WebContainer ourWebContainer = (WebContainer) bundleContext.getService(webContainerServiceReference);
                registerServlet(ourWebContainer, finalProperties);
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
        registerServlet(externalWebContainer,  obtainProperties());
        log.debug("Servlet with alias " + getAlias() + " registered back to external web container");
    }

    private String getAlias() {
        return alias;
    }

    /**
     * Code comes from org.apache.cxf.transport.http.osgi.ServletExporter#updated(java.util.Dictionary)
     * @param webContainer
     * @param properties
     */
    protected void registerServlet(WebContainer webContainer, Dictionary properties) {
        HttpContext httpContext = webContainer.createDefaultHttpContext();

        ServiceReference destinationServiceServiceReference = bundleContext.getServiceReference("org.apache.cxf.transport.http.DestinationRegistry");
        DestinationRegistry destinationRegistry = (DestinationRegistry) bundleContext.getService(destinationServiceServiceReference);

        Servlet servlet = new CXFNonSpringServlet(destinationRegistry, false);
        try {
            if (properties == null) {
                properties = new Properties();
            }
            Properties sprops = new Properties();
            sprops.put("init-prefix",
                    getProp(properties, CXF_SERVLET_PREFIX + "init-prefix", ""));
            sprops.put("servlet-name",
                    getProp(properties, CXF_SERVLET_PREFIX + "name", "cxf-osgi-transport-servlet"));
            sprops.put("hide-service-list-page",
                    getProp(properties, CXF_SERVLET_PREFIX + "hide-service-list-page", "false"));
            sprops.put("disable-address-updates",
                    getProp(properties, CXF_SERVLET_PREFIX + "disable-address-updates", "true"));
            sprops.put("base-address",
                    getProp(properties, CXF_SERVLET_PREFIX + "base-address", ""));
            sprops.put("service-list-path",
                    getProp(properties, CXF_SERVLET_PREFIX + "service-list-path", ""));
            sprops.put("static-resources-list",
                    getProp(properties, CXF_SERVLET_PREFIX + "static-resources-list", ""));
            sprops.put("redirects-list",
                    getProp(properties, CXF_SERVLET_PREFIX + "redirects-list", ""));
            sprops.put("redirect-servlet-name",
                    getProp(properties, CXF_SERVLET_PREFIX + "redirect-servlet-name", ""));
            sprops.put("redirect-servlet-path",
                    getProp(properties, CXF_SERVLET_PREFIX + "redirect-servlet-path", ""));
            sprops.put("service-list-all-contexts",
                    getProp(properties, CXF_SERVLET_PREFIX + "service-list-all-contexts", ""));
            sprops.put("service-list-page-authenticate",
                    getProp(properties, CXF_SERVLET_PREFIX + "service-list-page-authenticate", "false"));
            sprops.put("service-list-page-authenticate-realm",
                    getProp(properties, CXF_SERVLET_PREFIX + "service-list-page-authenticate-realm", "karaf"));
            sprops.put("use-x-forwarded-headers",
                    getProp(properties, CXF_SERVLET_PREFIX + "use-x-forwarded-headers", "false"));

            // Accept extra properties by default, can be disabled if it is really needed
            if (Boolean.valueOf(getProp(properties, CXF_SERVLET_PREFIX + "support.extra.properties", "true").toString())) {
                Enumeration keys = properties.keys();
                while (keys.hasMoreElements()) {
                    String nextKey = keys.nextElement().toString();
                    if (!nextKey.startsWith(CXF_SERVLET_PREFIX)) {
                        sprops.put(nextKey, properties.get(nextKey));
                    }
                }
            }

            Hashtable<String, Object> servletInitParams = new Hashtable<String, Object>();
            Enumeration keys = sprops.keys();

            while(keys.hasMoreElements()){
                String propName = (String) keys.nextElement();
                if (!FILTERED_PROPERTIES.contains(propName)) {
                    servletInitParams.put(propName, sprops.getProperty(propName));
                }
            }

            // Try to register servlet in given web container now
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
        BundleContext servletBundleContext = managedServiceReference.getBundle().getBundleContext();
        ServiceReference webContainerReference = servletBundleContext.getServiceReference(WebContainer.class.getName());
        if (webContainerReference == null) {
            log.warn("Not found webContainer reference for bundle " + servletBundleContext);
            return null;
        } else {
            return (WebContainer) servletBundleContext.getService(webContainerReference);
        }
    }

    private Dictionary obtainProperties(){
        Dictionary properties = null;
        ServiceReference reference = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
        ConfigurationAdmin admin = (ConfigurationAdmin) bundleContext.getService(reference);
        try {
            Configuration configuration = admin.getConfiguration("org.apache.cxf.osgi");
            properties = configuration.getProperties();
        } catch (Exception e){
            log.warn("Unable to obtain cxf osgi configadmin reference.", e);
        }
        return properties;
    }

    private Object getProp(Dictionary properties, String key, Object defaultValue) {
        Object value = null;
        if(properties != null){
            value = properties.get(key);
        }
        return value == null ? defaultValue : value;
    }

    public ServiceReference getManagedServiceReference() {
        return managedServiceReference;
    }

    public void setManagedServiceReference(ServiceReference managedServiceReference) {
        this.managedServiceReference = managedServiceReference;
    }
}
