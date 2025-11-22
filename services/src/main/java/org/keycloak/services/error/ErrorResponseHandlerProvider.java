package org.keycloak.services.error;

import jakarta.ws.rs.core.Response;
import org.keycloak.provider.Provider;

import java.util.Map;

public interface ErrorResponseHandlerProvider extends Provider {

  void prepareErrorContext(boolean isServerError, Response.Status httpStatus, Throwable errorCause);

  void enrichErrorPageModel(Map<String, Object> templateModel);
}

