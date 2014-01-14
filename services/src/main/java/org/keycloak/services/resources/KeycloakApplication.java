package org.keycloak.services.resources;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.SkeletonKeyContextResolver;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelProvider;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.SocialRequestManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.resources.admin.AdminService;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakApplication extends Application {

    private static final Logger log = Logger.getLogger(KeycloakApplication.class);

    private static final String MODEL_PROVIDER = "keycloak.model";
    private static final String DEFAULT_MODEL_PROVIDER = "jpa";

    protected Set<Object> singletons = new HashSet<Object>();
    protected Set<Class<?>> classes = new HashSet<Class<?>>();

    protected KeycloakSessionFactory factory;

    public KeycloakApplication(@Context ServletContext context) {
        this.factory = createSessionFactory();

        context.setAttribute(KeycloakSessionFactory.class.getName(), factory);
        //classes.add(KeycloakSessionCleanupFilter.class);

        TokenManager tokenManager = new TokenManager();

        singletons.add(new RealmsResource(tokenManager));
        singletons.add(new AdminService(tokenManager));
        singletons.add(new SocialResource(tokenManager, new SocialRequestManager()));
        classes.add(SkeletonKeyContextResolver.class);
        classes.add(QRCodeResource.class);

        setupDefaultRealm();
    }

    protected void setupDefaultRealm() {
        new ApplianceBootstrap().bootstrap(factory);
    }


    public static KeycloakSessionFactory createSessionFactory() {
        ServiceLoader<ModelProvider> providers = ServiceLoader.load(ModelProvider.class);
        String configuredProvider = System.getProperty(MODEL_PROVIDER);
        ModelProvider provider = null;

        if (configuredProvider != null) {
            for (ModelProvider p : providers) {
                if (p.getId().equals(configuredProvider)) {
                    provider = p;
                }
            }
        } else {
            for (ModelProvider p : providers) {
                if (provider == null) {
                    provider = p;
                }

                if (p.getId().equals(DEFAULT_MODEL_PROVIDER)) {
                    provider = p;
                    break;
                }
            }
        }

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
