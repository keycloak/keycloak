/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.admin.client;

import java.net.URI;
import java.util.Map;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * A Utility class that parses the Response object into the underlying ID attribute
 *
 * @author John D. Ament
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class CreatedResponseUtil {
    /**
     * Reads the Response object, confirms that it returns a 201 created and parses the ID from the location
     * It always assumes the ID is the last segment of the URI
     *
     * @param response The JAX-RS Response received
     * @return The String ID portion of the URI
     * @throws WebApplicationException if the response is not a 201 Created
     */
    public static String getCreatedId(Response response) throws WebApplicationException {
        URI location = response.getLocation();
        if (!response.getStatusInfo().equals(Response.Status.CREATED)) {
            Response.StatusType statusInfo = response.getStatusInfo();
            String contentType = response.getHeaderString(HttpHeaders.CONTENT_TYPE);
            String errorMessage = "Create method returned status " +
                                  statusInfo.getReasonPhrase() + " (Code: " + statusInfo.getStatusCode() + "); " +
                                  "expected status: Created (201).";
            try {
                if (matches(MediaType.APPLICATION_JSON_TYPE, MediaType.valueOf(contentType))) {
                    // try to add actual server error message to the exception message
                    @SuppressWarnings("raw")
                    Map responseBody = response.readEntity(Map.class);
                    if (responseBody != null) {
                        if (responseBody.containsKey("errorMessage")) {
                            errorMessage += " ErrorMessage: " + responseBody.get("errorMessage");
                        }
                        if (responseBody.containsKey("error")) {
                            errorMessage += " Error: " + responseBody.get("error");
                        }
                    }
                }
            } catch(Exception ignored){
                // ignore if we couldn't parse the response
            }

            throw new WebApplicationException(errorMessage, response);
        }
        if (location == null) {
            return null;
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private static boolean matches(MediaType a, MediaType b) {
        if (a == null) {
            return b == null;
        } else if (b == null) return false;

        return a.getType().equalsIgnoreCase(b.getType()) && a.getSubtype().equalsIgnoreCase(b.getSubtype());
    }
}
