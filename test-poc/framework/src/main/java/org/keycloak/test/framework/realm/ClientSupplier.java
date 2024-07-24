package org.keycloak.test.framework.realm;

import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.test.framework.annotations.InjectClient;
import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.injection.SupplierHelpers;

public class ClientSupplier implements Supplier<ClientResource, InjectClient> {

    private static final String CLIENT_UUID_KEY = "clientUuid";

    @Override
    public Class<InjectClient> getAnnotationClass() {
        return InjectClient.class;
    }

    @Override
    public Class<ClientResource> getValueType() {
        return ClientResource.class;
    }

    @Override
    public ClientResource getValue(InstanceContext<ClientResource, InjectClient> instanceContext) {
        RealmResource realm = instanceContext.getDependency(RealmResource.class);

        ClientConfig config = SupplierHelpers.getInstance(instanceContext.getAnnotation().config());
        ClientRepresentation clientRepresentation = config.getRepresentation();

        if (clientRepresentation.getClientId() == null) {
            String clientId = instanceContext.getRef();
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
    public boolean compatible(InstanceContext<ClientResource, InjectClient> a, RequestedInstance<ClientResource, InjectClient> b) {
        return a.getAnnotation().config().equals(b.getAnnotation().config());
    }

    @Override
    public void close(InstanceContext<ClientResource, InjectClient> instanceContext) {
        instanceContext.getValue().remove();
    }

}
