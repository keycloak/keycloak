package org.keycloak.services.resources;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.logging.Logger;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.Config;
import org.keycloak.SkeletonKeyContextResolver;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.DefaultKeycloakSessionFactory;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.UsersSyncManager;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.scheduled.ClearExpiredEvents;
import org.keycloak.services.scheduled.ClearExpiredUserSessions;
import org.keycloak.services.scheduled.ScheduledTaskRunner;
import org.keycloak.services.util.JsonConfigProvider;
import org.keycloak.timer.TimerProvider;
import org.keycloak.util.JsonSerialization;

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
import java.util.Map;
import java.util.Properties;
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

    protected KeycloakSessionFactory sessionFactory;
    protected String contextPath;

    public KeycloakApplication(@Context ServletContext context, @Context Dispatcher dispatcher) {
        loadConfig();

        this.sessionFactory = createSessionFactory();

        dispatcher.getDefaultContextObjects().put(KeycloakApplication.class, this);
        this.contextPath = context.getContextPath();
        BruteForceProtector protector = new BruteForceProtector(sessionFactory);
        dispatcher.getDefaultContextObjects().put(BruteForceProtector.class, protector);
        ResteasyProviderFactory.pushContext(BruteForceProtector.class, protector); // for injection
        protector.start();
        context.setAttribute(BruteForceProtector.class.getName(), protector);
        context.setAttribute(KeycloakSessionFactory.class.getName(), this.sessionFactory);

        singletons.add(new ServerVersionResource());
        singletons.add(new RealmsResource());
        singletons.add(new SocialResource());
        singletons.add(new AdminRoot());
        classes.add(SkeletonKeyContextResolver.class);
        classes.add(QRCodeResource.class);
        classes.add(ThemeResource.class);
        classes.add(JsResource.class);
        classes.add(WelcomeResource.class);

        new ExportImportManager().checkExportImport(this.sessionFactory);

        setupDefaultRealm(context.getContextPath());

        importRealms(context);
        setupScheduledTasks(sessionFactory);
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

    public static void loadConfig() {
        try {
            URL config = null;

            String configDir = System.getProperty("jboss.server.config.dir");
            if (configDir != null) {
                File f = new File(configDir + File.separator + "keycloak-server.json");
                if (f.isFile()) {
                    config = f.toURI().toURL();
                }
            }

            if (config == null) {
                config = Thread.currentThread().getContextClassLoader().getResource("META-INF/keycloak-server.json");
            }

            if (config != null) {
                JsonNode node = new ObjectMapper().readTree(config);

                Properties properties = new Properties();
                properties.putAll(System.getProperties());
                for(Map.Entry<String, String> e : System.getenv().entrySet()) {
                    properties.put("env." + e.getKey(), e.getValue());
                }

                Config.init(new JsonConfigProvider(node, properties));

                log.info("Loaded config from " + config);
                return;
            } else {
                log.warn("Config 'keycloak-server.json' not found");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config", e);
        }
    }

    protected void setupDefaultRealm(String contextPath) {
        new ApplianceBootstrap().bootstrap(sessionFactory, contextPath);
    }

    public static KeycloakSessionFactory createSessionFactory() {
        DefaultKeycloakSessionFactory factory = new DefaultKeycloakSessionFactory();
        factory.init();
        return factory;
    }

    public static void setupScheduledTasks(final KeycloakSessionFactory sessionFactory) {
        long interval = Config.scope("scheduled").getLong("interval", 60L) * 1000;

        TimerProvider timer = sessionFactory.create().getProvider(TimerProvider.class);
        timer.schedule(new ScheduledTaskRunner(sessionFactory, new ClearExpiredEvents()), interval, "ClearExpiredEvents");
        timer.schedule(new ScheduledTaskRunner(sessionFactory, new ClearExpiredUserSessions()), interval, "ClearExpiredUserSessions");
        new UsersSyncManager().bootstrapPeriodic(sessionFactory, timer);
    }

    public KeycloakSessionFactory getSessionFactory() {
        return sessionFactory;
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
        String files = System.getProperty("keycloak.import");
        if (files != null) {
            StringTokenizer tokenizer = new StringTokenizer(files, ",");
            while (tokenizer.hasMoreTokens()) {
                String file = tokenizer.nextToken().trim();
                RealmRepresentation rep = null;
                try {
                    rep = loadJson(new FileInputStream(file), RealmRepresentation.class);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                importRealm(rep, "file " + file);
            }
        }
    }

    public void importRealm(RealmRepresentation rep, String from) {
        KeycloakSession session = sessionFactory.create();
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

            try {
                RealmModel realm = manager.importRealm(rep);
                session.getTransaction().commit();
                log.info("Imported realm " + realm.getName() + " from " + from);
            } catch (Throwable t) {
                session.getTransaction().rollback();
                log.warn("Unable to import realm " + rep.getRealm() + " from " + from + ". Cause: " + t.getMessage());
            }
        } finally {
            session.close();
        }
    }

    private static <T> T loadJson(InputStream is, Class<T> type) {
        try {
            return JsonSerialization.readValue(is, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse json", e);
        }
    }

}
