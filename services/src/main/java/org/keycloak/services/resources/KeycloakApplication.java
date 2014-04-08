package org.keycloak.services.resources;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.SkeletonKeyContextResolver;
import org.keycloak.audit.AuditListener;
import org.keycloak.audit.AuditListenerFactory;
import org.keycloak.audit.AuditProvider;
import org.keycloak.audit.AuditProviderFactory;
import org.keycloak.authentication.AuthenticationProvider;
import org.keycloak.authentication.AuthenticationProviderFactory;
import org.keycloak.models.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelProvider;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ProviderFactoryLoader;
import org.keycloak.services.DefaultProviderSessionFactory;
import org.keycloak.picketlink.IdentityManagerProvider;
import org.keycloak.picketlink.IdentityManagerProviderFactory;
import org.keycloak.provider.ProviderSessionFactory;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.SocialRequestManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.resources.admin.AdminService;
import org.keycloak.models.utils.ModelProviderUtils;
import org.keycloak.timer.TimerProvider;
import org.keycloak.timer.TimerProviderFactory;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Date;
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
    protected ProviderSessionFactory providerSessionFactory;
    protected String contextPath;

    public KeycloakApplication(@Context ServletContext context) {
        this.factory = createSessionFactory();
        this.contextPath = context.getContextPath();
        this.providerSessionFactory = createProviderSessionFactory();
        context.setAttribute(KeycloakSessionFactory.class.getName(), factory);
        //classes.add(KeycloakSessionCleanupFilter.class);

        context.setAttribute(ProviderSessionFactory.class.getName(), this.providerSessionFactory);

        TokenManager tokenManager = new TokenManager();
        SocialRequestManager socialRequestManager = new SocialRequestManager();

        singletons.add(new RealmsResource(tokenManager, socialRequestManager));
        singletons.add(new AdminService(tokenManager));
        singletons.add(new SocialResource(tokenManager, socialRequestManager));
        classes.add(SkeletonKeyContextResolver.class);
        classes.add(QRCodeResource.class);
        classes.add(ThemeResource.class);

        setupDefaultRealm(context.getContextPath());

        setupScheduledTasks(providerSessionFactory, factory);
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

    protected void setupDefaultRealm(String contextPath) {
        new ApplianceBootstrap().bootstrap(factory, contextPath);
    }


    public static KeycloakSessionFactory createSessionFactory() {
        ModelProvider provider = ModelProviderUtils.getConfiguredModelProvider();

        if (provider != null) {
            log.debug("Model provider: " + provider.getId());
            return provider.createFactory();
        }

        throw new RuntimeException("Model provider not found");
    }

    public static DefaultProviderSessionFactory createProviderSessionFactory() {
        DefaultProviderSessionFactory factory = new DefaultProviderSessionFactory();

        factory.registerLoader(AuditProvider.class, ProviderFactoryLoader.create(AuditProviderFactory.class), Config.getAuditProvider());
        factory.registerLoader(AuditListener.class, ProviderFactoryLoader.create(AuditListenerFactory.class));
        factory.registerLoader(TimerProvider.class, ProviderFactoryLoader.create(TimerProviderFactory.class), Config.getTimerProvider());
        factory.registerLoader(IdentityManagerProvider.class, ProviderFactoryLoader.create(IdentityManagerProviderFactory.class), Config.getIdentityManagerProvider());
        factory.registerLoader(AuthenticationProvider.class, ProviderFactoryLoader.create(AuthenticationProviderFactory.class));
        factory.init();

        return factory;
    }

    public static void setupScheduledTasks(final ProviderSessionFactory providerSessionFactory, final KeycloakSessionFactory keycloakSessionFactory) {
        ProviderFactory<TimerProvider> timerFactory = providerSessionFactory.getProviderFactory(TimerProvider.class);
        if (timerFactory == null) {
            log.error("Can't setup schedule tasks, no timer provider found");
            return;
        }
        TimerProvider timer = timerFactory.create(null);

        final ProviderFactory<AuditProvider> auditFactory = providerSessionFactory.getProviderFactory(AuditProvider.class);
        if (auditFactory != null) {
            timer.schedule(new Runnable() {
                @Override
                public void run() {
                    KeycloakSession keycloakSession = keycloakSessionFactory.createSession();
                    AuditProvider audit = providerSessionFactory.getProviderFactory(AuditProvider.class).create(null);
                    try {
                        for (RealmModel realm : keycloakSession.getRealms()) {
                            if (realm.isAuditEnabled() && realm.getAuditExpiration() > 0) {
                                long olderThan = System.currentTimeMillis() - realm.getAuditExpiration() * 1000;
                                log.info("Expiring audit events for " + realm.getName() + " older than " + new Date(olderThan));
                                audit.clear(realm.getId(), olderThan);
                            }
                        }
                    } finally {
                        keycloakSession.close();
                        audit.close();
                    }
                }
            }, Config.getAuditExpirationSchedule());
        } else {
            log.info("Not scheduling audit expiration, no audit provider found");
        }
    }

    public KeycloakSessionFactory getFactory() {
        return factory;
    }

    public ProviderSessionFactory getProviderSessionFactory() {
        return providerSessionFactory;
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
