package org.keycloak.test.framework.realm;

import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.test.framework.annotations.InjectClient;
import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.injection.SupplierHelpers;
import org.keycloak.test.framework.util.ApiUtil;

public class ClientSupplier implements Supplier<ManagedClient, InjectClient> {

    @Override
    public Class<InjectClient> getAnnotationClass() {
        return InjectClient.class;
    }

    @Override
    public Class<ManagedClient> getValueType() {
        return ManagedClient.class;
    }

    @Override
    public ManagedClient getValue(InstanceContext<ManagedClient, InjectClient> instanceContext) {
        ManagedRealm realm = instanceContext.getDependency(ManagedRealm.class, instanceContext.getAnnotation().realmRef());

        ClientConfig config = SupplierHelpers.getInstance(instanceContext.getAnnotation().config());
        ClientRepresentation clientRepresentation = config.configure(ClientConfigBuilder.create()).build();

        if (clientRepresentation.getClientId() == null) {
            String clientId = SupplierHelpers.createName(instanceContext);
            clientRepresentation.setClientId(clientId);
        }

        if (instanceContext.getAnnotation().createClient()) {
            Response response = realm.admin().clients().create(clientRepresentation);
            if (Status.CONFLICT.equals(Status.fromStatusCode(response.getStatus()))) {
                throw new IllegalStateException("Client already exist with client id: " + clientRepresentation.getClientId());
            }
            clientRepresentation.setId(ApiUtil.handleCreatedResponse(response));
        } else {
            List<ClientRepresentation> clients = realm.admin().clients().findByClientId(clientRepresentation.getClientId());
            if (clients.isEmpty()) {
                throw new IllegalStateException("No client found with client id: " + clientRepresentation.getClientId());
            }
            clientRepresentation = clients.get(0);
        }

        ClientResource clientResource = realm.admin().clients().get(clientRepresentation.getId());
        return new ManagedClient(clientRepresentation, clientResource);
    }

    @Override
    public boolean compatible(InstanceContext<ManagedClient, InjectClient> a, RequestedInstance<ManagedClient, InjectClient> b) {
        return a.getAnnotation().config().equals(b.getAnnotation().config());
    }

    @Override
    public void close(InstanceContext<ManagedClient, InjectClient> instanceContext) {
        instanceContext.getValue().admin().remove();
    }

}
