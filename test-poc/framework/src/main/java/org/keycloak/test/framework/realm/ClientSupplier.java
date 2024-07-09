package org.keycloak.test.framework.realm;

import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.test.framework.TestClient;
import org.keycloak.test.framework.injection.InstanceWrapper;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.Registry;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.injection.SupplierHelpers;

public class ClientSupplier implements Supplier<ClientResource, TestClient> {

    private static final String CLIENT_UUID_KEY = "clientUuid";

    @Override
    public Class<TestClient> getAnnotationClass() {
        return TestClient.class;
    }

    @Override
    public Class<ClientResource> getValueType() {
        return ClientResource.class;
    }

    @Override
    public InstanceWrapper<ClientResource, TestClient> getValue(Registry registry, TestClient annotation) {
        InstanceWrapper<ClientResource, TestClient> wrapper = new InstanceWrapper<>(this, annotation);
        LifeCycle lifecycle = annotation.lifecycle();

        RealmResource realm = registry.getDependency(RealmResource.class, wrapper);

        ClientConfig config = SupplierHelpers.getInstance(annotation.config());
        ClientRepresentation clientRepresentation = config.getRepresentation();

        if (clientRepresentation.getClientId() == null) {
            String clientId = lifecycle.equals(LifeCycle.GLOBAL) ? config.getClass().getSimpleName() : registry.getCurrentContext().getRequiredTestClass().getSimpleName();
            clientRepresentation.setClientId(clientId);
        }

        Response response = realm.clients().create(clientRepresentation);

        String path = response.getLocation().getPath();
        String clientId = path.substring(path.lastIndexOf('/') + 1);

        response.close();

        wrapper.addNote(CLIENT_UUID_KEY, clientId);

        ClientResource clientResource = realm.clients().get(clientId);
        wrapper.setValue(clientResource, lifecycle);

        return wrapper;
    }

    @Override
    public boolean compatible(InstanceWrapper<ClientResource, TestClient> a, InstanceWrapper<ClientResource, TestClient> b) {
        return a.getAnnotation().config().equals(b.getAnnotation().config()) &&
                a.getNote(CLIENT_UUID_KEY, String.class).equals(b.getNote(CLIENT_UUID_KEY, String.class));
    }

    @Override
    public void close(ClientResource client) {
        client.remove();
    }

}
