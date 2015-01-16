package org.keycloak.adapters.osgi;

import java.util.Hashtable;

import javax.servlet.Servlet;

import org.jboss.logging.Logger;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;

/**
 * Service, which allows to remove previously registered servlets in karaf/fuse environment. It assumes that particular servlet was previously
 * registered as service in OSGI container under {@link javax.servlet.Servlet} interface.
 *
 * <p>The point is to register automatically registered builtin servlet endpoints (like "/cxf" for instance) to allow secure them
 * by Keycloak and re-register them again</p>
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ServletUnregistrationService {

    protected static final Logger log = Logger.getLogger(ServletUnregistrationService.class);

    private BundleContext bundleContext;
    private ServiceReference servletReference;

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

    // TODO: Re-register original servlet back during stop?
    public void start() {
        if (servletReference == null) {
            throw new IllegalStateException("No servlet reference provided");
        }

        Servlet servlet = (Servlet) bundleContext.getService(servletReference);
        WebContainer webContainer = findWebContainer(servletReference);
        if (webContainer == null) {
            return;
        }

        // Unregister servlet now
        try {
            webContainer.unregisterServlet(servlet);
            log.debugv("Original servlet with alias " + servletReference.getProperty("alias") + " unregistered successfully.");
        } catch (IllegalStateException e) {
            log.warnv("Can't unregister servlet due to: " + e.getMessage());
        }
    }

    public void stop() {
        try {
            Servlet servlet = (Servlet) bundleContext.getService(servletReference);
            WebContainer webContainer = findWebContainer(servletReference);
            if (webContainer == null) {
                return;
            }

            Hashtable<String, Object> servletInitParams = new Hashtable<String, Object>();
            String[] propNames = servletReference.getPropertyKeys();
            for (String propName : propNames) {
                servletInitParams.put(propName, servletReference.getProperty(propName));
            }

            // Try to register original servlet back
            HttpContext httpContext = webContainer.createDefaultHttpContext();
            String alias = (String) servletReference.getProperty("alias");
            webContainer.registerServlet(alias, servlet, servletInitParams, httpContext);
        } catch (Exception e) {
            log.warn("Can't register original servlet back", e);
        }
    }

    protected WebContainer findWebContainer(ServiceReference servletRef) {
        BundleContext servletBundleContext = servletRef.getBundle().getBundleContext();
        ServiceReference webContainerReference = servletBundleContext.getServiceReference(WebContainer.class.getName());
        if (webContainerReference == null) {
            log.warn("Not found webContainer reference for bundle " + servletBundleContext);
            return null;
        } else {
            return (WebContainer) servletBundleContext.getService(webContainerReference);
        }
    }

}
