package org.keycloak.testsuite.performance;

import org.apache.jmeter.samplers.SampleResult;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CreateUsersWorker implements Worker {

    private static final Logger log = LoggingManager.getLoggerForClass();

    private static final int NUMBER_OF_USERS_IN_EACH_REPORT = 5000;

    // Total number of users created during whole test
    private static AtomicInteger totalUserCounter = new AtomicInteger();

    // Adding users will always start from 1. Each worker thread needs to add users to single realm, which is dedicated just for this worker
    private int userCounterInRealm = 0;
    private String realmId;

    private int realmsOffset;
    private boolean addBasicUserAttributes;
    private boolean addDefaultRoles;
    private boolean addPassword;
    private int socialLinksPerUserCount;

    @Override
    public void setup(int workerId, KeycloakSession session) {
        realmsOffset = PerfTestUtils.readSystemProperty("keycloak.perf.createUsers.realms.offset", Integer.class);
        addBasicUserAttributes = PerfTestUtils.readSystemProperty("keycloak.perf.createUsers.addBasicUserAttributes", Boolean.class);
        addDefaultRoles = PerfTestUtils.readSystemProperty("keycloak.perf.createUsers.addDefaultRoles", Boolean.class);
        addPassword = PerfTestUtils.readSystemProperty("keycloak.perf.createUsers.addPassword", Boolean.class);
        socialLinksPerUserCount = PerfTestUtils.readSystemProperty("keycloak.perf.createUsers.socialLinksPerUserCount", Integer.class);

        int realmNumber = realmsOffset + workerId;
        realmId = PerfTestUtils.getRealmName(realmNumber);

        StringBuilder logBuilder = new StringBuilder("Read setup: ")
                .append("realmsOffset=" + realmsOffset)
                .append(", addBasicUserAttributes=" + addBasicUserAttributes)
                .append(", addDefaultRoles=" + addDefaultRoles)
                .append(", addPassword=" + addPassword)
                .append(", socialLinksPerUserCount=" + socialLinksPerUserCount)
                .append(", realmId=" + realmId);
        log.info(logBuilder.toString());
    }

    @Override
    public void run(SampleResult result, KeycloakSession session) {
        // We need to obtain realm first
        RealmModel realm = session.realms().getRealm(realmId);
        if (realm == null) {
            throw new IllegalStateException("Realm '" + realmId + "' not found");
        }

        int userNumber = ++userCounterInRealm;
        int totalUserNumber = totalUserCounter.incrementAndGet();

        String username = PerfTestUtils.getUsername(userNumber);

        UserModel user = session.users().addUser(realm, username);

        // Add basic user attributes (NOTE: Actually backend is automatically upgraded during each setter call)
        if (addBasicUserAttributes) {
            user.setFirstName(username + "FN");
            user.setLastName(username + "LN");
            user.setEmail(username + "@email.com");
        }

        // Creating password (will be same as username)
        if (addPassword) {
            UserCredentialModel password = new UserCredentialModel();
            password.setType(CredentialRepresentation.PASSWORD);
            password.setValue(username);
            user.updateCredential(password);
        }

        // Creating some socialLinks
        for (int i=0 ; i<socialLinksPerUserCount ; i++) {
            String socialProvider;
            switch (i) {
                case 0: socialProvider = "facebook"; break;
                case 1: socialProvider = "twitter"; break;
                case 2: socialProvider = "google"; break;
                default: throw new IllegalArgumentException("Total number of socialLinksPerUserCount is " + socialLinksPerUserCount
                        + " which is too big.");
            }

            SocialLinkModel socialLink = new SocialLinkModel(socialProvider, username, username);
            session.users().addSocialLink(realm, user, socialLink);
        }

        log.info("Finished creation of user " + username + " in realm: " + realm.getId());

        int labelC = ((totalUserNumber - 1) / NUMBER_OF_USERS_IN_EACH_REPORT) * NUMBER_OF_USERS_IN_EACH_REPORT;
        result.setSampleLabel("CreateUsers " + (labelC + 1) + "-" + (labelC + NUMBER_OF_USERS_IN_EACH_REPORT));
    }

    @Override
    public void tearDown() {
    }
}
