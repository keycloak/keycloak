/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.admin.client;

import org.apache.commons.lang3.ArrayUtils;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.ClientProvider;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.QUARKUS;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@AuthServerContainerExclude({REMOTE, QUARKUS})
public class ClientSearchTest extends AbstractClientTest {
    @ArquillianResource
    protected ContainerController controller;

    private static final String CLIENT1 = "client1";
    private static final String CLIENT2 = "client2";
    private static final String CLIENT3 = "client3";

    private String client1Id;
    private String client2Id;
    private String client3Id;

    private static final String ATTR_ORG_NAME = "org";
    private static final String ATTR_ORG_VAL = "Test_\"organisation\"";
    private static final String ATTR_URL_NAME = "url";
    private static final String ATTR_URL_VAL = "https://foo.bar/clflds";
    private static final String ATTR_QUOTES_NAME = "test \"123\"";
    private static final String ATTR_QUOTES_NAME_ESCAPED = "\"test \\\"123\\\"\"";
    private static final String ATTR_QUOTES_VAL = "field=\"blah blah\"";
    private static final String ATTR_QUOTES_VAL_ESCAPED = "\"field=\\\"blah blah\\\"\"";
    private static final String ATTR_FILTERED_NAME = "filtered";
    private static final String ATTR_FILTERED_VAL = "does_not_matter";

    private static final String SEARCHABLE_ATTRS_PROP = "keycloak.client.searchableAttributes";

    @Before
    public void init() {
        ClientRepresentation client1 = createOidcClientRep(CLIENT1);
        ClientRepresentation client2 = createOidcClientRep(CLIENT2);
        ClientRepresentation client3 = createOidcClientRep(CLIENT3);

        client1.setAttributes(new HashMap<String, String>() {{
            put(ATTR_ORG_NAME, ATTR_ORG_VAL);
            put(ATTR_URL_NAME, ATTR_URL_VAL);
        }});

        client2.setAttributes(new HashMap<String, String>() {{
            put(ATTR_URL_NAME, ATTR_URL_VAL);
            put(ATTR_FILTERED_NAME, ATTR_FILTERED_VAL);
        }});

        client3.setAttributes(new HashMap<String, String>() {{
            put(ATTR_ORG_NAME, "fake val");
            put(ATTR_QUOTES_NAME, ATTR_QUOTES_VAL);
        }});

        client1Id = createClient(client1);
        client2Id = createClient(client2);
        client3Id = createClient(client3);
    }

    @After
    public void teardown() {
        removeClient(client1Id);
        removeClient(client2Id);
        removeClient(client3Id);
    }

    @Test
    public void testQuerySearch() throws Exception {
        try {
            configureSearchableAttributes(ATTR_URL_NAME, ATTR_ORG_NAME, ATTR_QUOTES_NAME);
            search(String.format("%s:%s", ATTR_ORG_NAME, ATTR_ORG_VAL), CLIENT1);
            search(String.format("%s:%s", ATTR_URL_NAME, ATTR_URL_VAL), CLIENT1, CLIENT2);
            search(String.format("%s:%s %s:%s", ATTR_ORG_NAME, ATTR_ORG_VAL, ATTR_URL_NAME, ATTR_URL_VAL), CLIENT1);
            search(String.format("%s:%s %s:%s", ATTR_ORG_NAME, "wrong val", ATTR_URL_NAME, ATTR_URL_VAL));
            search(String.format("%s:%s", ATTR_QUOTES_NAME_ESCAPED, ATTR_QUOTES_VAL_ESCAPED), CLIENT3);

            // "filtered" attribute won't take effect when JPA is used
            String[] expectedRes = isJpaStore() ? new String[]{CLIENT1, CLIENT2} : new String[]{CLIENT2};
            search(String.format("%s:%s %s:%s", ATTR_URL_NAME, ATTR_URL_VAL, ATTR_FILTERED_NAME, ATTR_FILTERED_VAL), expectedRes);
        }
        finally {
            resetSearchableAttributes();
        }
    }

    @Test
    public void testJpaSearchableAttributesUnset() {
        String[] expectedRes = {CLIENT1};
        // JPA store removes all attributes by default, i.e. returns all clients
        if (isJpaStore()) {
            expectedRes = ArrayUtils.addAll(expectedRes, CLIENT2, CLIENT3, "account", "account-console", "admin-cli", "broker", "realm-management", "security-admin-console");
        }

        search(String.format("%s:%s", ATTR_ORG_NAME, ATTR_ORG_VAL), expectedRes);
    }

    private void search(String searchQuery, String... expectedClientIds) {
        List<String> found = testRealmResource().clients().query(searchQuery).stream()
                .map(ClientRepresentation::getClientId)
                .collect(Collectors.toList());
        assertThat(found, containsInAnyOrder(expectedClientIds));
    }

    void configureSearchableAttributes(String... searchableAttributes) throws Exception {
        log.infov("Configuring searchableAttributes");

        if (suiteContext.getAuthServerInfo().isUndertow()) {
            controller.stop(suiteContext.getAuthServerInfo().getQualifier());
            System.setProperty(SEARCHABLE_ATTRS_PROP, String.join(",", searchableAttributes));
            controller.start(suiteContext.getAuthServerInfo().getQualifier());
        } else if (suiteContext.getAuthServerInfo().isJBossBased()) {
            searchableAttributes = Arrays.stream(searchableAttributes).map(a -> a.replace("\"", "\\\\\\\"")).toArray(String[]::new);
            String s = "\\\"" + String.join("\\\",\\\"", searchableAttributes) + "\\\"";
            executeCli("/subsystem=keycloak-server/spi=client:add()",
                    "/subsystem=keycloak-server/spi=client/provider=jpa/:add(properties={searchableAttributes => \"[" + s + "]\"},enabled=true)");
        } else {
            throw new RuntimeException("Don't know how to config");
        }

        reconnectAdminClient();
    }

    void resetSearchableAttributes() throws Exception {
        log.info("Reset searchableAttributes");

        if (suiteContext.getAuthServerInfo().isUndertow()) {
            controller.stop(suiteContext.getAuthServerInfo().getQualifier());
            System.clearProperty(SEARCHABLE_ATTRS_PROP);
            controller.start(suiteContext.getAuthServerInfo().getQualifier());
        } else if (suiteContext.getAuthServerInfo().isJBossBased()) {
            executeCli("/subsystem=keycloak-server/spi=client:remove");
        } else {
            throw new RuntimeException("Don't know how to config");
        }

        reconnectAdminClient();
    }

    private void executeCli(String... commands) throws Exception {
        OnlineManagementClient client = AuthServerTestEnricher.getManagementClient();
        Administration administration = new Administration(client);

        log.debug("Running CLI commands:");
        for (String c : commands) {
            log.debug(c);
            client.execute(c).assertSuccess();
        }
        log.debug("Done");

        administration.reload();

        client.close();
    }

    private boolean isJpaStore() {
        String providerId = testingClient.server()
                .fetchString(s -> s.getKeycloakSessionFactory().getProviderFactory(ClientProvider.class).getId());
        log.info("Detected store: " + providerId);
        return "\"jpa\"".equals(providerId); // there are quotes for some reason
    }
}
