package org.keycloak.services.resources;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.SkeletonKeyContextResolver;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelProvider;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.SocialRequestManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.resources.admin.AdminService;
import org.keycloak.models.utils.ModelProviderUtils;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakApplication extends Application {

    private static final Logger log = Logger.getLogger(KeycloakApplication.class);

    protected Set<Object> singletons = new HashSet<Object>();
    protected Set<Class<?>> classes = new HashSet<Class<?>>();

    protected KeycloakSessionFactory factory;
    protected String contextPath;

    public KeycloakApplication(@Context ServletContext context) {
        this.factory = createSessionFactory();
        this.contextPath = context.getContextPath();
        context.setAttribute(KeycloakSessionFactory.class.getName(), factory);
        //classes.add(KeycloakSessionCleanupFilter.class);

        TokenManager tokenManager = new TokenManager();
        SocialRequestManager socialRequestManager = new SocialRequestManager();

        singletons.add(new RealmsResource(tokenManager, socialRequestManager));
        singletons.add(new AdminService(tokenManager));
        singletons.add(new SocialResource(tokenManager, socialRequestManager));
        classes.add(SkeletonKeyContextResolver.class);
        classes.add(QRCodeResource.class);
        classes.add(ThemeResource.class);

        setupDefaultRealm();
    }

    public String getContextPath() {
        return contextPath;
    }

    /**
     * Get base URI of WAR distribution, not JAX-RS
     *
     * @param uriInfo
     * @return
     */
    public URI getBaseUri(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().replacePath(getContextPath()).build();
    }

    protected void setupDefaultRealm() {
        new ApplianceBootstrap().bootstrap(factory);
    }


    public static KeycloakSessionFactory createSessionFactory() {
        ModelProvider provider = ModelProviderUtils.getConfiguredModelProvider();

        if (provider != null) {
            log.debug("Model provider: " + provider.getId());
            return provider.createFactory();
        }

        throw new RuntimeException("Model provider not found");
    }

    public KeycloakSessionFactory getFactory() {
        return factory;
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

}
