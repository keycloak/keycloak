package org.keycloak.services.resources;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.Config;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.migration.MigrationModelManager;
import org.keycloak.models.*;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.DefaultKeycloakSessionFactory;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.UsersSyncManager;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.scheduled.ClearExpiredEvents;
import org.keycloak.services.scheduled.ClearExpiredUserSessions;
import org.keycloak.services.scheduled.ScheduledTaskRunner;
import org.keycloak.services.util.JsonConfigProvider;
import org.keycloak.services.util.ObjectMapperResolver;
import org.keycloak.timer.TimerProvider;
import org.keycloak.util.JsonSerialization;
import org.keycloak.common.util.SystemEnvProperties;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakApplication extends Application {

    private static final ServicesLogger log = ServicesLogger.ROOT_LOGGER;

    protected Set<Object> singletons = new HashSet<Object>();
    protected Set<Class<?>> classes = new HashSet<Class<?>>();

    protected KeycloakSessionFactory sessionFactory;
    protected String contextPath;

    public KeycloakApplication(@Context ServletContext context, @Context Dispatcher dispatcher) {
        loadConfig();

        this.contextPath = context.getContextPath();
        this.sessionFactory = createSessionFactory();

        dispatcher.getDefaultContextObjects().put(KeycloakApplication.class, this);
        BruteForceProtector protector = new BruteForceProtector(sessionFactory);
        dispatcher.getDefaultContextObjects().put(BruteForceProtector.class, protector);
        ResteasyProviderFactory.pushContext(BruteForceProtector.class, protector); // for injection
        ResteasyProviderFactory.pushContext(KeycloakApplication.class, this); // for injection
        protector.start();
        context.setAttribute(BruteForceProtector.class.getName(), protector);
        context.setAttribute(KeycloakSessionFactory.class.getName(), this.sessionFactory);

        singletons.add(new ServerVersionResource());
        singletons.add(new RealmsResource());
        singletons.add(new AdminRoot());
        singletons.add(new ModelExceptionMapper());
        classes.add(QRCodeResource.class);
        classes.add(ThemeResource.class);
        classes.add(JsResource.class);

        singletons.add(new ObjectMapperResolver(Boolean.parseBoolean(System.getProperty("keycloak.jsonPrettyPrint", "false"))));

        migrateModel();
        sessionFactory.publish(new PostMigrationEvent());

        boolean bootstrapAdminUser = false;

        KeycloakSession session = sessionFactory.create();
        ExportImportManager exportImportManager;
        try {
            session.getTransaction().begin();

            ApplianceBootstrap applianceBootstrap = new ApplianceBootstrap(session);
            exportImportManager = new ExportImportManager(session);

            boolean createMasterRealm = applianceBootstrap.isNewInstall();
            if (exportImportManager.isRunImport() && exportImportManager.isImportMasterIncluded()) {
                createMasterRealm = false;
            }

            if (createMasterRealm) {
                applianceBootstrap.createMasterRealm(contextPath);
            }
            session.getTransaction().commit();
        } finally {
            session.close();
        }

        if (exportImportManager.isRunImport()) {
            exportImportManager.runImport();
        } else {
            importRealms();
        }

        importAddUser();

        if (exportImportManager.isRunExport()) {
            exportImportManager.runExport();
        }

        session = sessionFactory.create();
        try {
            session.getTransaction().begin();
            bootstrapAdminUser = new ApplianceBootstrap(session).isNoMasterUser();

            session.getTransaction().commit();
        } finally {
            session.close();
        }

        singletons.add(new WelcomeResource(bootstrapAdminUser));

        setupScheduledTasks(sessionFactory);
    }

    protected void migrateModel() {
        KeycloakSession session = sessionFactory.create();
        try {
            session.getTransaction().begin();
            MigrationModelManager.migrate(session);
            session.getTransaction().commit();
        } catch (Exception e) {
            session.getTransaction().rollback();
            log.migrationFailure(e);
        } finally {
            session.close();
        }
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
            JsonNode node = null;

            String configDir = System.getProperty("jboss.server.config.dir");
            if (configDir != null) {
                File f = new File(configDir + File.separator + "keycloak-server.json");
                if (f.isFile()) {
                    log.loadingFrom(f.getAbsolutePath());
                    node = new ObjectMapper().readTree(f);
                }
            }

            if (node == null) {
                URL resource = Thread.currentThread().getContextClassLoader().getResource("META-INF/keycloak-server.json");
                if (resource != null) {
                    log.loadingFrom(resource);
                    node = new ObjectMapper().readTree(resource);
                }
            }

            if (node != null) {
                Properties properties = new SystemEnvProperties();
                Config.init(new JsonConfigProvider(node, properties));
                return;
            } else {
                throw new RuntimeException("Config 'keycloak-server.json' not found");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config", e);
        }
    }

    public static KeycloakSessionFactory createSessionFactory() {
        DefaultKeycloakSessionFactory factory = new DefaultKeycloakSessionFactory();
        factory.init();
        return factory;
    }

    public static void setupScheduledTasks(final KeycloakSessionFactory sessionFactory) {
        long interval = Config.scope("scheduled").getLong("interval", 60L) * 1000;

        KeycloakSession session = sessionFactory.create();
        try {
            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.schedule(new ScheduledTaskRunner(sessionFactory, new ClearExpiredEvents()), interval, "ClearExpiredEvents");
            timer.schedule(new ScheduledTaskRunner(sessionFactory, new ClearExpiredUserSessions()), interval, "ClearExpiredUserSessions");
            new UsersSyncManager().bootstrapPeriodic(sessionFactory, timer);
        } finally {
            session.close();
        }
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

    public void importRealms() {
        String files = System.getProperty("keycloak.import");
        if (files != null) {
            StringTokenizer tokenizer = new StringTokenizer(files, ",");
            while (tokenizer.hasMoreTokens()) {
                String file = tokenizer.nextToken().trim();
                RealmRepresentation rep;
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
                log.realmExists(rep.getRealm(), from);
                return;
            }

            if (manager.getRealmByName(rep.getRealm()) != null) {
                log.realmExists(rep.getRealm(), from);
                return;
            }

            try {
                RealmModel realm = manager.importRealm(rep);
                session.getTransaction().commit();
                log.importedRealm(realm.getName(), from);
            } catch (Throwable t) {
                session.getTransaction().rollback();
                log.unableToImportRealm(t, rep.getRealm(), from);
            }
        } finally {
            session.close();
        }
    }

    public void importAddUser() {
        String configDir = System.getProperty("jboss.server.config.dir");
        if (configDir != null) {
            File addUserFile = new File(configDir + File.separator + "keycloak-add-user.json");
            if (addUserFile.isFile()) {
                log.imprtingUsersFrom(addUserFile);

                List<RealmRepresentation> realms;
                try {
                    realms = JsonSerialization.readValue(new FileInputStream(addUserFile), new TypeReference<List<RealmRepresentation>>() {
                    });
                } catch (IOException e) {
                    log.failedToLoadUsers(e);
                    return;
                }

                for (RealmRepresentation realmRep : realms) {
                    for (UserRepresentation userRep : realmRep.getUsers()) {
                        KeycloakSession session = sessionFactory.create();
                        try {
                            session.getTransaction().begin();

                            RealmModel realm = session.realms().getRealmByName(realmRep.getRealm());
                            if (realm == null) {
                                log.addUserFailedRealmNotFound(userRep.getUsername(), realmRep.getRealm());
                            } else {
                                UserModel user = session.users().addUser(realm, userRep.getUsername());
                                user.setEnabled(userRep.isEnabled());
                                RepresentationToModel.createCredentials(userRep, user);
                                RepresentationToModel.createRoleMappings(userRep, user, realm);
                            }

                            session.getTransaction().commit();
                            log.addUserSuccess(userRep.getUsername(), realmRep.getRealm());
                        } catch (ModelDuplicateException e) {
                            log.addUserFailedUserExists(userRep.getUsername(), realmRep.getRealm());
                        } catch (Throwable t) {
                            session.getTransaction().rollback();
                            log.addUserFailed(t, userRep.getUsername(), realmRep.getRealm());
                        } finally {
                            session.close();
                        }
                    }
                }

                if (!addUserFile.delete()) {
                    log.failedToDeleteFile(addUserFile.getAbsolutePath());
                }
            }
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
