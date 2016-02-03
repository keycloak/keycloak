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

import java.net.URL;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.util.security.Constraint;
import org.jboss.logging.Logger;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Integration with pax-web, which allows to inject custom jetty-web.xml configuration from current bundle classpath into {@link WebContainer}
 * and allows to inject custom security constraint for securing resources by Keycloak.
 *
 * <p>It assumes that pax-web {@link WebContainer} is used as implementation of OSGI {@link org.osgi.service.http.HttpService}, which
 * is true in karaf/fuse environment</p>
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PaxWebIntegrationService {

    protected static final Logger log = Logger.getLogger(PaxWebIntegrationService.class);

    private BundleContext bundleContext;
    private String jettyWebXmlLocation;
    private List<ConstraintMapping> constraintMappings; // Using jetty constraint mapping just because of compatibility with other fuse services

    private ServiceTracker webContainerTracker;
    private HttpContext httpContext;

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public String getJettyWebXmlLocation() {
        return jettyWebXmlLocation;
    }

    public void setJettyWebXmlLocation(String jettyWebXmlLocation) {
        this.jettyWebXmlLocation = jettyWebXmlLocation;
    }

    public List<ConstraintMapping> getConstraintMappings() {
        return constraintMappings;
    }

    public void setConstraintMappings(List<ConstraintMapping> constraintMappings) {
        this.constraintMappings = constraintMappings;
    }

    protected ServiceTracker getWebContainerTracker() {
        return webContainerTracker;
    }

    protected HttpContext getHttpContext() {
        return httpContext;
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
        httpContext = service.createDefaultHttpContext();

        addJettyWebXml(service);

        if (constraintMappings == null) {
            throw new IllegalStateException("constraintMappings was null!");
        }
        for (ConstraintMapping constraintMapping : constraintMappings) {
            addConstraintMapping(service, constraintMapping);
        }

        service.registerLoginConfig("BASIC", "does-not-matter", null, null, httpContext);

        return service;
    }

    protected void addJettyWebXml(WebContainer service) {
        String jettyWebXmlLoc;
        if (this.jettyWebXmlLocation == null) {
            jettyWebXmlLoc = "/WEB-INF/jetty-web.xml";
        } else {
            jettyWebXmlLoc = this.jettyWebXmlLocation;
        }

        URL jettyWebXml = bundleContext.getBundle().getResource(jettyWebXmlLoc);
        if (jettyWebXml != null) {
            log.debug("Found jetty-web XML configuration on bundle classpath on " + jettyWebXmlLoc);
            service.registerJettyWebXml(jettyWebXml, httpContext);
        } else {
            log.debug("Not found jetty-web XML configuration on bundle classpath on " + jettyWebXmlLoc);
        }
    }

    protected void addConstraintMapping(WebContainer service, ConstraintMapping constraintMapping) {
        Constraint constraint = constraintMapping.getConstraint();
        String[] roles = constraint.getRoles();
        // name property is unavailable on constraint object :/

        String name = "Constraint-" + new SecureRandom().nextInt(Integer.MAX_VALUE);

        int dataConstraint = constraint.getDataConstraint();
        String dataConstraintStr;
        switch (dataConstraint) {
            case Constraint.DC_UNSET: dataConstraintStr = null; break;
            case Constraint.DC_NONE: dataConstraintStr = "NONE"; break;
            case Constraint.DC_CONFIDENTIAL: dataConstraintStr = "CONFIDENTIAL"; break;
            case Constraint.DC_INTEGRAL: dataConstraintStr = "INTEGRAL"; break;
            default:
                log.warnv("Unknown data constraint: " + dataConstraint);
                dataConstraintStr = "CONFIDENTIAL";
        }
        List<String> rolesList = Arrays.asList(roles);

        log.debug("Adding security constraint name=" + name + ", url=" + constraintMapping.getPathSpec() + ", dataConstraint=" + dataConstraintStr + ", canAuthenticate="
        + constraint.getAuthenticate() + ", roles=" + rolesList);
        service.registerConstraintMapping(name, constraintMapping.getPathSpec(), null, dataConstraintStr, constraint.getAuthenticate(), rolesList, httpContext);
    }

    protected void removingWebContainerCallback(ServiceReference serviceReference) {
        WebContainer service = (WebContainer)bundleContext.getService(serviceReference);
        if (service != null) {
            service.unregisterLoginConfig(httpContext);
            service.unregisterConstraintMapping(httpContext);
        }
    }
}
