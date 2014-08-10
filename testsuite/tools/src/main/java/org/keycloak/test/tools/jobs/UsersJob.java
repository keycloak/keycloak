package org.keycloak.test.tools.jobs;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.test.tools.PerfTools;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class UsersJob implements Runnable {

    protected PerfTools.JobRepresentation job;
    protected KeycloakSessionFactory sessionFactory;
    protected String realmName;
    protected int start;
    protected int count;
    protected String prefix;
    protected CountDownLatch latch;

    public void init(PerfTools.JobRepresentation job, KeycloakSessionFactory sessionFactory, String realmName, int start, int count, String prefix, CountDownLatch latch) {
        this.sessionFactory = sessionFactory;
        this.realmName = realmName;
        this.start = start;
        this.count = count;
        this.prefix = prefix;
        this.job = job;
        this.latch = latch;

        KeycloakSession session = sessionFactory.create();
        try {
            session.getTransaction().begin();

            before(session);

            session.getTransaction().commit();
        } catch (Throwable t) {
            handleThrowable(t, session);
        } finally {
            session.close();
        }
    }

    @Override
    public void run() {
        job.start();

        KeycloakSession session = sessionFactory.create();
        try {
            session.getTransaction().begin();

            RealmModel realm = new RealmManager(session).getRealmByName(realmName);
            Map<String, ApplicationModel> apps = realm.getApplicationNameMap();

            Set<RoleModel> realmRoles = realm.getRoles();
            Map<String, Set<RoleModel>> appRoles = new HashMap<String, Set<RoleModel>>();
            for (Map.Entry<String, ApplicationModel> appEntry : apps.entrySet()) {
                appRoles.put(appEntry.getKey(), appEntry.getValue().getRoles());
            }

            for (int i = start; i < (start + count); i++) {
                runIteration(session, realm, apps, realmRoles, appRoles, i);
                job.increment();
            }

            session.getTransaction().commit();
        } catch (Throwable t) {
            handleThrowable(t, session);
        } finally {
            latch.countDown();
            session.close();
        }

    }

    protected abstract void before(KeycloakSession keycloakSession);

    protected abstract void runIteration(KeycloakSession session, RealmModel realm, Map<String, ApplicationModel> apps, Set<RoleModel> realmRoles, Map<String, Set<RoleModel>> appRoles, int counter);

    protected RoleModel findRole(Set<RoleModel> roles, String roleName) {
        for (RoleModel role : roles) {
            if (role.getName().equals(roleName)) {
                return role;
            }
        }

        return null;
    }

    protected void grantRole(UserModel user, String roleName, Set<RoleModel> realmRoles, Map<String, Set<RoleModel>> appRoles) {
        if (roleName.indexOf(':') == -1) {
            // We expect "realmRoleName"
            RoleModel realmRole = findRole(realmRoles, roleName);
            user.grantRole(realmRole);
        } else {
            // We expect "appName:appRoleName"
            String[] parts = roleName.split(":");
            Set<RoleModel> currentAppRoles = appRoles.get(parts[0]);
            if (currentAppRoles == null) {
                throw new IllegalStateException("Application '" + parts[0] + "' not found");
            }

            RoleModel appRole = findRole(currentAppRoles, parts[1]);
            user.grantRole(appRole);
        }
    }

    private void handleThrowable(Throwable t, KeycloakSession session) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        job.setError(sw.toString());
        session.getTransaction().rollback();
    }

}
