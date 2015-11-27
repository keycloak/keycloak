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
    public static final String REFRESH_TOKEN = "refresh_token"; // Not officially supported by OIDC
    public static final String NONE = "none";

    private static final List<String> ALLOWED_RESPONSE_TYPES = Arrays.asList(CODE, TOKEN, ID_TOKEN, REFRESH_TOKEN, NONE);

    private final List<String> responseTypes;


    private OIDCResponseType(List<String> responseTypes) {
        this.responseTypes = responseTypes;
    }


    public static OIDCResponseType parse(String responseTypeParam) {
        if (responseTypeParam == null) {
            throw new IllegalStateException("response_type is null");
        }

        String[] responseTypes = responseTypeParam.trim().split(" ");
        List<String> allowedTypes = new ArrayList<>();
        for (String current : responseTypes) {
            if (ALLOWED_RESPONSE_TYPES.contains(current)) {
                allowedTypes.add(current);
            } else {
                throw new IllegalStateException("Unsupported response_type: " + responseTypeParam);
            }
        }
        return new OIDCResponseType(allowedTypes);
    }


    public boolean hasResponseType(String responseType) {
        return responseTypes.contains(responseType);
    }


    public boolean isImplicitOrHybridFlow() {
        return hasResponseType(TOKEN) || hasResponseType(ID_TOKEN) || hasResponseType(REFRESH_TOKEN);
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
