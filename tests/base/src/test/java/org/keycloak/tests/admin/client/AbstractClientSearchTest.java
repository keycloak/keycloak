package org.keycloak.tests.admin.client;

import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class AbstractClientSearchTest {

    @InjectRealm(config = ClientSearchRealmConfig.class)
    ManagedRealm testRealm;

    static final String CLIENT_ID_1 = "client1";
    static final String CLIENT_ID_2 = "client2";
    static final String CLIENT_ID_3 = "client3";

    static final String ATTR_ORG_NAME = "org";
    static final String ATTR_ORG_VAL = "Test_\"organisation\"";
    static final String ATTR_URL_NAME = "url";
    static final String ATTR_URL_VAL = "https://foo.bar/clflds";
    static final String ATTR_QUOTES_NAME = "test \"123\"";
    static final String ATTR_QUOTES_NAME_ESCAPED = "\"test \\\"123\\\"\"";
    static final String ATTR_QUOTES_VAL = "field=\"blah blah\"";
    static final String ATTR_QUOTES_VAL_ESCAPED = "\"field=\\\"blah blah\\\"\"";
    static final String ATTR_FILTERED_NAME = "filtered";
    static final String ATTR_FILTERED_VAL = "does_not_matter";

    void search(String searchQuery, String... expectedClientIds) {
        List<String> found = testRealm.admin().clients().query(searchQuery).stream()
                .map(ClientRepresentation::getClientId)
                .collect(Collectors.toList());
        assertThat(found, containsInAnyOrder(expectedClientIds));
    }

    public static class ClientSearchRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.addClient(CLIENT_ID_1)
                    .attribute(ATTR_ORG_NAME, ATTR_ORG_VAL)
                    .attribute(ATTR_URL_NAME, ATTR_URL_VAL);

            realm.addClient(CLIENT_ID_2)
                    .attribute(ATTR_URL_NAME, ATTR_URL_VAL)
                    .attribute(ATTR_FILTERED_NAME, ATTR_FILTERED_VAL);

            realm.addClient(CLIENT_ID_3)
                    .attribute(ATTR_ORG_NAME, "fake val")
                    .attribute(ATTR_QUOTES_NAME, ATTR_QUOTES_VAL);

            return realm;
        }
    }
}
