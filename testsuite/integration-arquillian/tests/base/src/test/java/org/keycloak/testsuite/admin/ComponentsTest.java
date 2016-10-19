/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.admin;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ComponentsResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.testsuite.components.TestProvider;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ComponentsTest extends AbstractAdminTest {

    private ComponentsResource components;

    @Before
    public void before() throws Exception {
        components = adminClient.realm(REALM_NAME).components();
    }

    @Test
    public void testNotDeadlocked() {
        for (int i = 0; i < 100; i++) {
            ComponentRepresentation rep = createComponentRepresentation("test-" + i);
            rep.getConfig().putSingle("required", "required-value");
            createComponent(rep);

            List<ComponentRepresentation> list = realm.components().query(realmId, TestProvider.class.getName());
            assertEquals(i + 1, list.size());
        }
    }

    @Test
    public void testCreateValidation() {
        ComponentRepresentation rep = createComponentRepresentation("mycomponent");

        // Check validation is invoked
        try {
            createComponent(rep);
        } catch (WebApplicationException e) {
            assertErrror(e.getResponse(), "Required is required");
        }

        rep.getConfig().putSingle("required", "Required");
        rep.getConfig().putSingle("number", "invalid");

        // Check validation is invoked
        try {
            createComponent(rep);
        } catch (WebApplicationException e) {
            assertErrror(e.getResponse(), "Number should be a number");
        }
    }

    @Test
    public void testCreateEmptyValues() {
        ComponentRepresentation rep = createComponentRepresentation("mycomponent");

        rep.getConfig().addFirst("required", "foo");
        rep.getConfig().addFirst("val1", "");
        rep.getConfig().put("val2", null);
        rep.getConfig().put("val3", Collections.emptyList());

        String id = createComponent(rep);
        ComponentRepresentation returned = components.component(id).toRepresentation();
        assertEquals( "foo", returned.getSubType());

        assertEquals(1, returned.getConfig().size());
        assertTrue(returned.getConfig().containsKey("required"));
    }

    @Test
    public void testUpdate() {
        ComponentRepresentation rep = createComponentRepresentation("mycomponent");

        rep.getConfig().addFirst("required", "foo");
        rep.getConfig().addFirst("val1", "one");
        rep.getConfig().addFirst("val2", "two");
        rep.getConfig().addFirst("val3", "three");

        String id = createComponent(rep);
        ComponentRepresentation returned = components.component(id).toRepresentation();
        assertEquals(4, returned.getConfig().size());
        assertEquals("foo", returned.getConfig().getFirst("required"));
        assertEquals("one", returned.getConfig().getFirst("val1"));
        assertEquals("two", returned.getConfig().getFirst("val2"));
        assertEquals("three", returned.getConfig().getFirst("val3"));

        // Check value updated
        returned.getConfig().putSingle("val1", "one-updated");

        // Check null deletes property
        returned.getConfig().putSingle("val2", null);

        components.component(id).update(returned);

        returned = components.component(id).toRepresentation();
        assertEquals(3, returned.getConfig().size());
        assertEquals("one-updated", returned.getConfig().getFirst("val1"));
        assertFalse(returned.getConfig().containsKey("val2"));

        // Check empty string is deleted
        returned.getConfig().addFirst("val1", "");

        components.component(id).update(returned);

        returned = components.component(id).toRepresentation();
        assertEquals(2, returned.getConfig().size());

        // Check empty list removes property
        returned.getConfig().put("val3", Collections.emptyList());

        components.component(id).update(returned);

        returned = components.component(id).toRepresentation();
        assertEquals(1, returned.getConfig().size());
    }

    @Test
    public void testSecretConfig() throws Exception {
        ComponentRepresentation rep = createComponentRepresentation("mycomponent");
        rep.getConfig().addFirst("secret", "some secret value!!");
        rep.getConfig().addFirst("required", "some required value");

        String id = createComponent(rep);

        // Check secret value is not returned
        ComponentRepresentation returned = components.component(id).toRepresentation();
        assertEquals(ComponentRepresentation.SECRET_VALUE, returned.getConfig().getFirst("secret"));

        Map<String, TestProvider.DetailsRepresentation> details = testingClient.testing(REALM_NAME).getTestComponentDetails();

        // Check value is set correctly
        assertEquals("some secret value!!", details.get("mycomponent").getConfig().get("secret").get(0));

        returned.getConfig().putSingle("priority", "200");
        components.component(id).update(returned);

        ComponentRepresentation returned2 = components.component(id).toRepresentation();
        assertEquals(ComponentRepresentation.SECRET_VALUE, returned2.getConfig().getFirst("secret"));

        // Check secret value is not set to '*********'
        details = testingClient.testing(REALM_NAME).getTestComponentDetails();
        assertEquals("some secret value!!", details.get("mycomponent").getConfig().get("secret").get(0));

        returned2.getConfig().putSingle("secret", "updated secret value!!");
        components.component(id).update(returned2);

        // Check secret value is updated
        details = testingClient.testing(REALM_NAME).getTestComponentDetails();
        assertEquals("updated secret value!!", details.get("mycomponent").getConfig().get("secret").get(0));
    }

    private String createComponent(ComponentRepresentation rep) {
        ComponentsResource components = realm.components();
        Response response = components.add(rep);
        String id = ApiUtil.getCreatedId(response);
        response.close();
        return id;
    }

    private void assertErrror(Response response, String error) {
        if (!response.hasEntity()) {
            fail("No error message set");
        }

        ErrorRepresentation errorRepresentation = response.readEntity(ErrorRepresentation.class);
        assertEquals(error, errorRepresentation.getErrorMessage());
    }

    private ComponentRepresentation createComponentRepresentation(String name) {
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName(name);
        rep.setParentId(realmId);
        rep.setProviderId("test");
        rep.setProviderType(TestProvider.class.getName());
        rep.setSubType("foo");

        MultivaluedHashMap config = new MultivaluedHashMap();
        rep.setConfig(config);
        return rep;
    }

}
