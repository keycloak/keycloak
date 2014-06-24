package org.keycloak.test.tools.jobs;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.test.tools.PerfTools;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class CreateUsers implements Runnable {

    private PerfTools.Job job;
    private final KeycloakSessionFactory sessionFactory;
    private final String realmName;
    private int start;
    private int count;
    private String prefix;
    private String[] roles;

    public CreateUsers(PerfTools.Job job, KeycloakSessionFactory sessionFactory, String realmName, int start, int count, String prefix, String[] roles) {
        this.job = job;
        this.sessionFactory = sessionFactory;
        this.realmName = realmName;
        this.start = start;
        this.count = count;
        this.prefix = prefix;
        this.roles = roles;
    }

    @Override
    public void run() {
        job.start();

        KeycloakSession session = sessionFactory.create();
        try {
            session.getTransaction().begin();

            RealmModel realm = new RealmManager(session).getRealmByName(realmName);

            for (int i = start; i < (start + count); i++) {
                UserModel user = realm.addUser(prefix + "-" + i);
                user.setEnabled(true);
                user.setFirstName("First");
                user.setLastName("Last");
                user.setEmail(prefix + "-" + i + "@localhost");

                UserCredentialModel password = new UserCredentialModel();
                password.setType(UserCredentialModel.PASSWORD);
                password.setValue("password");

                user.updateCredential(password);

                for (String r : roles) {
                    user.grantRole(realm.getRole(r));
                }

                job.increment();
            }

            session.getTransaction().commit();
        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            job.setError(sw.toString());
        } finally {
            session.close();
        }
    }

}
