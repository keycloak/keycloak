package org.keycloak.services.resources;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.messages.MessageProvider;

/**
 * @author <a href="mailto:leonardo.zanivan@gmail.com">Leonardo Zanivan</a>
 */
@Provider
public class KeycloakExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger logger = Logger.getLogger(KeycloakExceptionMapper.class);

    @Context
    private KeycloakSession session;

    @Context
    private MessageProvider messageProvider;

    @Override
    public Response toResponse(Exception ex) {
        if (ex instanceof ModelException) {
            ModelException mex = (ModelException) ex;
            String message = messageProvider.getMessage(session, mex.getMessage(), mex.getParameters());
            return ErrorResponse.error(message, Response.Status.BAD_REQUEST);
        } else {
            logger.error("Unhandled exception", ex);
            return ErrorResponse.error("Server error. See server.log for details", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

}
