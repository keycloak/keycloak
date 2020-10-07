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

package org.keycloak.adapters.osgi.jetty94;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.util.security.Constraint;
import org.jboss.logging.Logger;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.model.SecurityConstraintMappingModel;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private List<Object> constraintMappings;

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

    public List<Object> getConstraintMappings() {
        return constraintMappings;
    }

    public void setConstraintMappings(List<Object> constraintMappings) {
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

        addJettyWebXml(service);

        if (constraintMappings == null) {
            throw new IllegalStateException("constraintMappings was null!");
        }
        List<ConstraintHandler> handlers = new ArrayList<>();
        try {
            handlers.add(new JettyConstraintHandler());
        } catch (Throwable t) {
            // Ignore
        }
        try {
            handlers.add(new PaxWebConstraintHandler());
        } catch (Throwable t) {
            // Ignore
        }
        for (Object constraintMapping : constraintMappings) {
            boolean handled = false;
            for (ConstraintHandler handler : handlers) {
                handled |= handler.addConstraintMapping(httpContext, service, constraintMapping);
            }
            if (!handled) {
                log.warnv("Unable to add constraint mapping for constraint of type " + constraintMapping.getClass().toString());
            }
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

    protected void addConstraintMapping(WebContainer service, SecurityConstraintMappingModel constraintMapping) {
        String name = constraintMapping.getConstraintName();
        if (name == null) {
            name = "Constraint-" + new SecureRandom().nextInt(Integer.MAX_VALUE);
        }
        log.debug("Adding security constraint name=" + name + ", url=" + constraintMapping.getUrl() + ", dataConstraint=" + constraintMapping.getDataConstraint() + ", canAuthenticate="
                + constraintMapping.isAuthentication() + ", roles=" + constraintMapping.getRoles());
        service.registerConstraintMapping(name, constraintMapping.getMapping(), constraintMapping.getUrl(), constraintMapping.getDataConstraint(), constraintMapping.isAuthentication(), constraintMapping.getRoles(), httpContext);
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
        service.registerConstraintMapping(name, null, constraintMapping.getPathSpec(), dataConstraintStr, constraint.getAuthenticate(), rolesList, httpContext);
    }

    protected void removingWebContainerCallback(ServiceReference serviceReference) {
        WebContainer service = (WebContainer)bundleContext.getService(serviceReference);
        if (service != null) {
            service.unregisterLoginConfig(httpContext);
            service.unregisterConstraintMapping(httpContext);
        }
    }

    private interface ConstraintHandler {
        boolean addConstraintMapping(HttpContext httpContext, WebContainer service, Object cm);
    }

    private static class PaxWebConstraintHandler implements ConstraintHandler {

        public boolean addConstraintMapping(HttpContext httpContext, WebContainer service, Object cm) {
            if (cm instanceof SecurityConstraintMappingModel) {
                SecurityConstraintMappingModel constraintMapping = (SecurityConstraintMappingModel) cm;
                String name = constraintMapping.getConstraintName();
                if (name == null) {
                    name = "Constraint-" + new SecureRandom().nextInt(Integer.MAX_VALUE);
                }
                log.debug("Adding security constraint name=" + name + ", url=" + constraintMapping.getUrl() + ", dataConstraint=" + constraintMapping.getDataConstraint() + ", canAuthenticate="
                        + constraintMapping.isAuthentication() + ", roles=" + constraintMapping.getRoles());
                service.registerConstraintMapping(name, constraintMapping.getMapping(), constraintMapping.getUrl(), constraintMapping.getDataConstraint(), constraintMapping.isAuthentication(), constraintMapping.getRoles(), httpContext);
                return true;
            }
            return false;
        }

    }

    private static class JettyConstraintHandler implements ConstraintHandler {

        public boolean addConstraintMapping(HttpContext httpContext, WebContainer service, Object cm) {
            if (cm instanceof ConstraintMapping) {
                ConstraintMapping constraintMapping = (ConstraintMapping) cm;
                Constraint constraint = constraintMapping.getConstraint();
                String[] roles = constraint.getRoles();
                // name property is unavailable on constraint object :/

                String name = "Constraint-" + new SecureRandom().nextInt(Integer.MAX_VALUE);

                int dataConstraint = constraint.getDataConstraint();
                String dataConstraintStr;
                switch (dataConstraint) {
                    case Constraint.DC_UNSET:
                        dataConstraintStr = null;
                        break;
                    case Constraint.DC_NONE:
                        dataConstraintStr = "NONE";
                        break;
                    case Constraint.DC_CONFIDENTIAL:
                        dataConstraintStr = "CONFIDENTIAL";
                        break;
                    case Constraint.DC_INTEGRAL:
                        dataConstraintStr = "INTEGRAL";
                        break;
                    default:
                        log.warnv("Unknown data constraint: " + dataConstraint);
                        dataConstraintStr = "CONFIDENTIAL";
                }
                List<String> rolesList = Arrays.asList(roles);

                log.debug("Adding security constraint name=" + name + ", url=" + constraintMapping.getPathSpec() + ", dataConstraint=" + dataConstraintStr + ", canAuthenticate="
                        + constraint.getAuthenticate() + ", roles=" + rolesList);
                service.registerConstraintMapping(name, null, constraintMapping.getPathSpec(), dataConstraintStr, constraint.getAuthenticate(), rolesList, httpContext);
                return true;
            }
            return false;
        }

    }
}
