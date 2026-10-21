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

package org.keycloak.protocol.oidc.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.keycloak.protocol.oidc.OIDCLoginProtocol;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCResponseType {

    public static final String CODE = OIDCLoginProtocol.CODE_PARAM;
    public static final String TOKEN = "token";
    public static final String ID_TOKEN = "id_token";
    public static final String NONE = "none";

    private static final List<String> ALLOWED_RESPONSE_TYPES = Arrays.asList(CODE, TOKEN, ID_TOKEN, NONE);

    private final List<String> responseTypes;


    private OIDCResponseType(List<String> responseTypes) {
        this.responseTypes = responseTypes;
    }


    public static OIDCResponseType parse(String responseTypeParam) {
        if (responseTypeParam == null) {
            throw new IllegalArgumentException("response_type is null");
        }

        String[] responseTypes = responseTypeParam.trim().split(" ");
        List<String> allowedTypes = new ArrayList<>();
        for (String current : responseTypes) {
            if (ALLOWED_RESPONSE_TYPES.contains(current)) {
                allowedTypes.add(current);
            } else {
                throw new IllegalArgumentException("Unsupported response_type");
            }
        }

        validateAllowedTypes(allowedTypes);

        return new OIDCResponseType(allowedTypes);
    }

    public static OIDCResponseType parse(List<String> responseTypes) {
        OIDCResponseType result = new OIDCResponseType(new ArrayList<String>());
        for (String respType : responseTypes) {
            OIDCResponseType responseType = parse(respType);
            result.responseTypes.addAll(responseType.responseTypes);
        }

        return result;
    }

    private static void validateAllowedTypes(List<String> responseTypes) {
        if (responseTypes.size() == 0) {
            throw new IllegalStateException("No responseType provided");
        }
        if (responseTypes.contains(NONE) && responseTypes.size() > 1) {
            throw new IllegalArgumentException("'None' not allowed with some other response_type");
        }

        // response_type value "token" alone is not mentioned in OIDC specification, however it is supported by OAuth2. We allow it just to be compatible with pure OAuth2 clients like swagger.ui
//        if (responseTypes.contains(TOKEN) && responseTypes.size() == 1) {
//            throw new IllegalArgumentException("Not supported to use response_type=token alone");
//        }
    }


    public boolean hasResponseType(String responseType) {
        return responseTypes.contains(responseType);
    }

    /**
     * Checks whether the given {@code responseType} is the only value within the requested response types.
     *
     * @param responseType the response type
     * @return {@code true} if the given response type if within the list of response types. Otherwise, {@code false}
     */
    public boolean hasSingleResponseType(String responseType) {
        if (responseTypes.size() > 1) {
            return false;
        }
        return responseTypes.contains(responseType);
    }


    public boolean isImplicitOrHybridFlow() {
        return hasResponseType(TOKEN) || hasResponseType(ID_TOKEN);
    }

    public boolean isImplicitFlow() {
        return (hasResponseType(TOKEN) || hasResponseType(ID_TOKEN)) && !hasResponseType(CODE);
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String responseType : responseTypes) {
            if (!first) {
                builder.append(" ");
            } else {
                first = false;
            }
            builder.append(responseType);
        }
        return builder.toString();
    }
}
