package org.keycloak.testsuite.performance;

import org.apache.jmeter.samplers.SampleResult;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RemoveUsersWorker implements Worker {

    private static final Logger log = LoggingManager.getLoggerForClass();

    private static final int NUMBER_OF_USERS_IN_EACH_REPORT = 5000;

    // Total number of users removed during whole test
    private static AtomicInteger totalUserCounter = new AtomicInteger();

    // Removing users will always start from 1. Each worker thread needs to add users to single realm, which is dedicated just for this worker
    private int userCounterInRealm = 0;
    private RealmModel realm;

    private int realmsOffset;

    @Override
    public void setup(int workerId, KeycloakSession session) {
        realmsOffset = PerfTestUtils.readSystemProperty("keycloak.perf.removeUsers.realms.offset", Integer.class);

        int realmNumber = realmsOffset + workerId;
        String realmId = PerfTestUtils.getRealmName(realmNumber);
        realm = session.realms().getRealm(realmId);
        if (realm == null) {
            throw new IllegalStateException("Realm '" + realmId + "' not found");
        }

        log.info("Read setup: realmsOffset=" + realmsOffset);
    }

    @Override
    public void run(SampleResult result, KeycloakSession session) {
        throw new IllegalStateException("Not yet supported");
        /*
        int userNumber = ++userCounterInRealm;
        int totalUserNumber = totalUserCounter.incrementAndGet();

        String username = PerfTestUtils.getUsername(userNumber);

        // TODO: Not supported in model actually. We support operation just in MongoDB
        // UserModel user = realm.removeUser(username);
        if (PropertiesManager.isMongoSessionFactory()) {
            RealmAdapter mongoRealm = (RealmAdapter)realm;
            mongoRealm.removeUser(username);
        } else {
            throw new IllegalArgumentException("Actually removing of users is supported just for MongoDB");
        }

        log.info("Finished removing of user " + username + " in realm: " + realm.getId());

        int labelC = ((totalUserNumber - 1) / NUMBER_OF_USERS_IN_EACH_REPORT) * NUMBER_OF_USERS_IN_EACH_REPORT;
        result.setSampleLabel("ReadUsers " + (labelC + 1) + "-" + (labelC + NUMBER_OF_USERS_IN_EACH_REPORT));
        */
    }

    @Override
    public void tearDown() {
    }
}
