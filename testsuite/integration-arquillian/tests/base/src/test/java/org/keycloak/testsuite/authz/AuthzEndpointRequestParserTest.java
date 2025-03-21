package org.keycloak.testsuite.authz;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.Matchers;

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

            String loginUrl = oauth.loginForm()
                    .param("paramkey1_too_long", RandomStringUtils.random(2000 + 1))
                    .param("paramkey2", "paramvalue2")
                    .param("paramkey3", "paramvalue3")
                    .param("paramkey4", "paramvalue4")
                    .param("paramkey5", "paramvalue5")
                    .param("paramkey6_too_many", "paramvalue6").build();

            try (Response response = client.target(loginUrl).request().get()) {

                assertThat(response.getStatus(), is(equalTo(200)));
                assertThat(response, Matchers.body(containsString("Sign in")));

            }

        }

    }

}
