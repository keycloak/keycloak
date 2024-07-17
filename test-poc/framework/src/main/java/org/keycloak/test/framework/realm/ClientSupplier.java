package org.keycloak.test.framework.realm;

import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.test.framework.TestClient;
import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.RequestedInstance;
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
    public ClientResource getValue(InstanceContext<ClientResource, TestClient> instanceContext) {
        RealmResource realm = instanceContext.getDependency(RealmResource.class);

        ClientConfig config = SupplierHelpers.getInstance(instanceContext.getAnnotation().config());
        ClientRepresentation clientRepresentation = config.getRepresentation();

        if (clientRepresentation.getClientId() == null) {
            String clientId = instanceContext.getLifeCycle().equals(LifeCycle.GLOBAL) ? config.getClass().getSimpleName() : instanceContext.getRegistry().getCurrentContext().getRequiredTestClass().getSimpleName();
            clientRepresentation.setClientId(clientId);
        }

        Response response = realm.clients().create(clientRepresentation);

        String path = response.getLocation().getPath();
        String clientId = path.substring(path.lastIndexOf('/') + 1);

        response.close();

        instanceContext.addNote(CLIENT_UUID_KEY, clientId);

        return realm.clients().get(clientId);
    }

    @Override
    public boolean compatible(InstanceContext<ClientResource, TestClient> a, RequestedInstance<ClientResource, TestClient> b) {
        return a.getAnnotation().config().equals(b.getAnnotation().config());
    }

    @Override
    public void close(InstanceContext<ClientResource, TestClient> instanceContext) {
        instanceContext.getValue().remove();
    }

}
