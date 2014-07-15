package org.keycloak.testsuite.performance;

import org.apache.jmeter.samplers.SampleResult;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.RealmManager;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CreateRealmsWorker implements Worker {

    private static final Logger log = LoggingManager.getLoggerForClass();

    private static final int NUMBER_OF_REALMS_IN_EACH_REPORT = 100;

    private static AtomicInteger realmCounter = new AtomicInteger(0);

    private int offset;
    private int appsPerRealm;
    private int rolesPerRealm;
    private int defaultRolesPerRealm;
    private int rolesPerApp;
    private boolean createRequiredCredentials;

    @Override
    public void setup(int workerId, KeycloakSession session) {
        offset = PerfTestUtils.readSystemProperty("keycloak.perf.createRealms.realms.offset", Integer.class);
        appsPerRealm = PerfTestUtils.readSystemProperty("keycloak.perf.createRealms.appsPerRealm", Integer.class);
        rolesPerRealm = PerfTestUtils.readSystemProperty("keycloak.perf.createRealms.rolesPerRealm", Integer.class);
        defaultRolesPerRealm = PerfTestUtils.readSystemProperty("keycloak.perf.createRealms.defaultRolesPerRealm", Integer.class);
        rolesPerApp = PerfTestUtils.readSystemProperty("keycloak.perf.createRealms.rolesPerApp", Integer.class);
        createRequiredCredentials = PerfTestUtils.readSystemProperty("keycloak.perf.createRealms.createRequiredCredentials", Boolean.class);

        realmCounter.compareAndSet(0, offset);

        StringBuilder logBuilder = new StringBuilder("Read setup: ")
                .append("offset=" + offset)
                .append(", appsPerRealm=" + appsPerRealm)
                .append(", rolesPerRealm=" + rolesPerRealm)
                .append(", defaultRolesPerRealm=" + defaultRolesPerRealm)
                .append(", rolesPerApp=" + rolesPerApp)
                .append(", createRequiredCredentials=" + createRequiredCredentials);
        log.info(logBuilder.toString());
    }

    @Override
    public void run(SampleResult result, KeycloakSession session) {
        int realmNumber = realmCounter.getAndIncrement();
        String realmName = PerfTestUtils.getRealmName(realmNumber);
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.createRealm(realmName, realmName);

        // Add roles
        for (int i=1 ; i<=rolesPerRealm ; i++) {
            realm.addRole(PerfTestUtils.getRoleName(realmNumber, i));
        }

        // Add default roles
        for (int i=1 ; i<=defaultRolesPerRealm ; i++) {
            realm.addDefaultRole(PerfTestUtils.getDefaultRoleName(realmNumber, i));
        }

        // Add applications
        for (int i=1 ; i<=appsPerRealm ; i++) {
            ApplicationModel application = realm.addApplication(PerfTestUtils.getApplicationName(realmNumber, i));
            for (int j=1 ; j<=rolesPerApp ; j++) {
                application.addRole(PerfTestUtils.getApplicationRoleName(realmNumber, i, j));
            }
        }

        // Add required credentials
        if (createRequiredCredentials) {
            RepresentationToModel.addRequiredCredential(realm, CredentialRepresentation.PASSWORD);
            RepresentationToModel.addRequiredCredential(realm, CredentialRepresentation.TOTP);
            RepresentationToModel.addRequiredCredential(realm, CredentialRepresentation.CLIENT_CERT);
        }

        log.info("Finished creation of realm " + realmName);

        int labelC = ((realmNumber - 1) / NUMBER_OF_REALMS_IN_EACH_REPORT) * NUMBER_OF_REALMS_IN_EACH_REPORT;
        result.setSampleLabel("CreateRealms " + (labelC + 1) + "-" + (labelC + NUMBER_OF_REALMS_IN_EACH_REPORT));
    }

    @Override
    public void tearDown() {
    }

}
