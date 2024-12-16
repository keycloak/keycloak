package org.keycloak.testsuite.authz;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.RealmBuilder;

import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class AuthzEndpointRequestParserTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Test
    public void test_authentication_backwards_compatible() {

        try (Client client = AdminClientUtil.createResteasyClient()) {

            oauth.addCustomParameter("paramkey1_too_long", RandomStringUtils.random(2000 + 1));
            oauth.addCustomParameter("paramkey2", "paramvalue2");
            oauth.addCustomParameter("paramkey3", "paramvalue3");
            oauth.addCustomParameter("paramkey4", "paramvalue4");
            oauth.addCustomParameter("paramkey5", "paramvalue5");
            oauth.addCustomParameter("paramkey6_too_many", "paramvalue6");

            try (Response response = client.target(oauth.getLoginFormUrl()).request().get()) {

                assertThat(response.getStatus(), is(equalTo(200)));
                assertThat(response, Matchers.body(containsString("Sign in")));

            }

        }

    }

}
