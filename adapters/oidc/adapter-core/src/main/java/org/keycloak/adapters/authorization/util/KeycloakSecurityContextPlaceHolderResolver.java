/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.adapters.authorization.util;

import static org.keycloak.adapters.authorization.util.PlaceHolders.getParameter;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class KeycloakSecurityContextPlaceHolderResolver implements PlaceHolderResolver {

    public static final String NAME = "keycloak";

    @Override
    public List<String> resolve(String placeHolder, HttpFacade httpFacade) {
        String source = placeHolder.substring(placeHolder.indexOf('.') + 1);
        OIDCHttpFacade oidcHttpFacade = OIDCHttpFacade.class.cast(httpFacade);
        KeycloakSecurityContext securityContext = oidcHttpFacade.getSecurityContext();

        if (securityContext == null) {
            return null;
        }

        if (source.endsWith("access_token")) {
            return Arrays.asList(securityContext.getTokenString());
        }

        if (source.endsWith("id_token")) {
            return Arrays.asList(securityContext.getIdTokenString());
        }

        JsonNode jsonNode;

        if (source.startsWith("access_token[")) {
            jsonNode = JsonSerialization.mapper.valueToTree(securityContext.getToken());
        } else if (source.startsWith("id_token[")) {
            jsonNode = JsonSerialization.mapper.valueToTree(securityContext.getIdToken());
        } else {
            throw new RuntimeException("Invalid placeholder [" + placeHolder + "]");
        }

        return JsonUtils.getValues(jsonNode, getParameter(source, "Invalid placeholder [" + placeHolder + "]"));
    }
}
