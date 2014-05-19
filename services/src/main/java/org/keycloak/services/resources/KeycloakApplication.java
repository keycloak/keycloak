package org.keycloak.services.resources;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.logging.Logger;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.Config;
import org.keycloak.SkeletonKeyContextResolver;
import org.keycloak.exportimport.ExportImportProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderSession;
import org.keycloak.provider.ProviderSessionFactory;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.DefaultProviderSessionFactory;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.SocialRequestManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.scheduled.ClearExpiredAuditEvents;
import org.keycloak.services.scheduled.ClearExpiredUserSessions;
import org.keycloak.services.scheduled.ScheduledTaskRunner;
import org.keycloak.services.util.JsonConfigProvider;
import org.keycloak.timer.TimerProvider;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.ProviderLoader;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakApplication extends Application {

    private static final Logger log = Logger.getLogger(KeycloakApplication.class);

    protected Set<Object> singletons = new HashSet<Object>();
    protected Set<Class<?>> classes = new HashSet<Class<?>>();

    protected ProviderSessionFactory providerSessionFactory;
    protected String contextPath;

    public KeycloakApplication(@Context ServletContext context, @Context Dispatcher dispatcher) {
        loadConfig();

        this.providerSessionFactory = createProviderSessionFactory();

        dispatcher.getDefaultContextObjects().put(KeycloakApplication.class, this);
        this.contextPath = context.getContextPath();
        BruteForceProtector protector = new BruteForceProtector(providerSessionFactory);
        dispatcher.getDefaultContextObjects().put(BruteForceProtector.class, protector);
        ResteasyProviderFactory.pushContext(BruteForceProtector.class, protector); // for injection
        protector.start();
        context.setAttribute(BruteForceProtector.class.getName(), protector);
        context.setAttribute(ProviderSessionFactory.class.getName(), this.providerSessionFactory);

        TokenManager tokenManager = new TokenManager();
        SocialRequestManager socialRequestManager = new SocialRequestManager();

        singletons.add(new RealmsResource(tokenManager, socialRequestManager));
        singletons.add(new SocialResource(tokenManager, socialRequestManager));
        singletons.add(new AdminRoot(tokenManager));
        classes.add(SkeletonKeyContextResolver.class);
        classes.add(QRCodeResource.class);
        classes.add(ThemeResource.class);
        classes.add(JsResource.class);
        classes.add(WelcomeResource.class);

        setupDefaultRealm(context.getContextPath());

        setupScheduledTasks(providerSessionFactory);
        importRealms(context);

        checkExportImportProvider();
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

    protected void loadConfig() {
        try {
            URL config = Thread.currentThread().getContextClassLoader().getResource("META-INF/keycloak-server.json");

            if (config != null) {
                JsonNode node = new ObjectMapper().readTree(config);
                Config.init(new JsonConfigProvider(node));

                log.info("Loaded config from " + config);
                return;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config", e);
        }
    }

    protected void setupDefaultRealm(String contextPath) {
        new ApplianceBootstrap().bootstrap(providerSessionFactory, contextPath);
    }

    public static DefaultProviderSessionFactory createProviderSessionFactory() {
        DefaultProviderSessionFactory factory = new DefaultProviderSessionFactory();
        factory.init();
        return factory;
    }

    public static void setupScheduledTasks(final ProviderSessionFactory providerSessionFactory) {
        long interval = Config.scope("scheduled").getLong("interval", 60L) * 1000;

        TimerProvider timer = providerSessionFactory.createSession().getProvider(TimerProvider.class);
        timer.schedule(new ScheduledTaskRunner(providerSessionFactory, new ClearExpiredAuditEvents()), interval);
        timer.schedule(new ScheduledTaskRunner(providerSessionFactory, new ClearExpiredUserSessions()), interval);
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

    public void importRealms(ServletContext context) {
        importRealmFile();
        importRealmResources(context);
    }

    public void importRealmResources(ServletContext context) {
        String resources = context.getInitParameter("keycloak.import.realm.resources");
        if (resources != null) {
            StringTokenizer tokenizer = new StringTokenizer(resources, ",");
            while (tokenizer.hasMoreTokens()) {
                String resource = tokenizer.nextToken().trim();
                InputStream is = context.getResourceAsStream(resource);
                if (is == null) {
                    log.warn("Could not find realm resource to import: " + resource);
                }
                RealmRepresentation rep = loadJson(is, RealmRepresentation.class);
                importRealm(rep, "resource " + resource);
            }
        }
    }

    public void importRealmFile() {
        String file = System.getProperty("keycloak.import");
        if (file != null) {
            RealmRepresentation rep = null;
            try {
                rep = loadJson(new FileInputStream(file), RealmRepresentation.class);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            importRealm(rep, "file " + file);
        }
    }

    public void importRealm(RealmRepresentation rep, String from) {
        ProviderSession providerSession = providerSessionFactory.createSession();
        KeycloakSession session = providerSession.getProvider(KeycloakSession.class);
        try {
            session.getTransaction().begin();
            RealmManager manager = new RealmManager(session);
            manager.setContextPath(getContextPath());

            if (rep.getId() != null && manager.getRealm(rep.getId()) != null) {
                log.info("Not importing realm " + rep.getRealm() + " from " + from + ".  It already exists.");
                return;
            }

            if (manager.getRealmByName(rep.getRealm()) != null) {
                log.info("Not importing realm " + rep.getRealm() + " from " + from + ".  It already exists.");
                return;
            }

            RealmModel realm = manager.createRealm(rep.getId(), rep.getRealm());
            manager.importRealm(rep, realm);

            log.info("Imported realm " + realm.getName() + " from " + from);

            session.getTransaction().commit();
        } finally {
            providerSession.close();
        }
    }

    private static <T> T loadJson(InputStream is, Class<T> type) {
        try {
            return JsonSerialization.readValue(is, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse json", e);
        }
    }

    protected void checkExportImportProvider() {
        Iterator<ExportImportProvider> providers = ProviderLoader.load(ExportImportProvider.class).iterator();

        if (providers.hasNext()) {
            ExportImportProvider exportImport = providers.next();
            exportImport.checkExportImport(providerSessionFactory);
        } else {
            log.warn("No ExportImportProvider found!");
        }
    }


}
