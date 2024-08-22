package org.keycloak.test.framework.realm;

import jakarta.ws.rs.core.Response;
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
        ClientRepresentation clientRepresentation = config.getRepresentation();

        if (clientRepresentation.getClientId() == null) {
            String clientId = SupplierHelpers.createName(instanceContext);
            clientRepresentation.setClientId(clientId);
        }

        Response response = realm.admin().clients().create(clientRepresentation);
        String uuid = ApiUtil.handleCreatedResponse(response);
        clientRepresentation.setId(uuid);

        ClientResource clientResource = realm.admin().clients().get(uuid);
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
