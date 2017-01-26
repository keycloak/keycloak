package org.keycloak.testsuite.runonserver;

import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;

/**
 * Created by st on 26.01.17.
 */
public class InternalComponentRepresentation implements FetchOnServerWrapper<ComponentRepresentation> {

    private final String componentId;

    public InternalComponentRepresentation(String componentId) {
        this.componentId = componentId;
    }

    @Override
    public FetchOnServer getRunOnServer() {
        return (FetchOnServer) session -> ModelToRepresentation.toRepresentation(session.getContext().getRealm(), true);
    }

    @Override
    public Class<ComponentRepresentation> getResultClass() {
        return ComponentRepresentation.class;
    }

}
