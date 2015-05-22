package org.keycloak.services.resources;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.keycloak.messages.MessagesProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.services.ErrorResponse;

/**
 * @author <a href="mailto:leonardo.zanivan@gmail.com">Leonardo Zanivan</a>
 */
@Provider
public class ModelExceptionMapper implements ExceptionMapper<ModelException> {

    @Context
    private KeycloakSession session;

    @Override
    public Response toResponse(ModelException ex) {
        String message = session.getProvider(MessagesProvider.class, "admin")
                .getMessage(ex.getMessage(), ex.getParameters());
        return ErrorResponse.error(message, Response.Status.BAD_REQUEST);
    }
}
