package org.keycloak.testsuite.federation.testConnection;

import java.util.List;
import java.util.function.Consumer;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.AbstractAuthTest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class UserStorageTestConnectionTest extends AbstractAuthTest {

    private static final String PROVIDER_NAME = "test-connection-dummy";

    @Test
    public void testConnectionSuccess() {
        String componentId = createProviderModel(model -> {
            model.getConfig().putSingle(TestConnectionStorageProviderFactory.CONFIG_URL, "https://www.keycloak.org/");
        });

        //The test connection must be successful
        testRealmResource().userStorage().testConnection(componentId);
    }

    @Test
    public void testConnectionSuccessWhenDisabled() {
        String componentId = createProviderModel(model -> {
            model.getConfig().putSingle(TestConnectionStorageProviderFactory.CONFIG_URL, "https://www.keycloak.org/");
            model.getConfig().putSingle("enabled", "false");
        });

        //The test connection must be successful
        testRealmResource().userStorage().testConnection(componentId);
    }

    @Test
    public void testConnectionTestFailureResult() {
        String componentId = createProviderModel();

        //The test connection must be fail: Url property is missing
        Assert.assertThrows(BadRequestException.class, () -> {
            testRealmResource().userStorage().testConnection(componentId);
        });
    }

    @Test
    public void testConnectionTestFailureOnException() {
        String componentId = createProviderModel(model -> {
            model.getConfig().putSingle(TestConnectionStorageProviderFactory.CONFIG_URL, "://invalid");
        });

        //The test connection must be fail: expected to throw MalformedURLException
        Assert.assertThrows(BadRequestException.class, () -> {
            testRealmResource().userStorage().testConnection(componentId);
        });
    }

    @After
    public void cleanup() {
        String realmId = testRealmResource().toRepresentation().getId();
        List<ComponentRepresentation> models = testRealmResource().components().query(realmId,
                UserStorageProvider.class.getName(), PROVIDER_NAME);
        for (ComponentRepresentation model : models) {
            testRealmResource().components().removeComponent(model.getId());
        }
    }

    private String createProviderModel() {
        return createProviderModel(null);
    }

    private String createProviderModel(Consumer<ComponentRepresentation> consumer) {
        ComponentRepresentation model = new ComponentRepresentation();
        String componentId = KeycloakModelUtils.generateId();

        model.setId(componentId);
        model.setProviderId(TestConnectionStorageProviderFactory.PROVIDER_ID);
        model.setProviderType(UserStorageProvider.class.getName());
        model.setConfig(new MultivaluedHashMap<>());
        model.setName(PROVIDER_NAME);

        if (consumer != null) {
            consumer.accept(model);
        }

        Response resp = testRealmResource().components().add(model);
        Assert.assertEquals(Response.Status.CREATED.getStatusCode(), resp.getStatus());

        return componentId;
    }
}
