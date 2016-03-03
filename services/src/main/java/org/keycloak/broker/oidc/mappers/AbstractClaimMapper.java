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

package org.keycloak.broker.oidc.mappers;

import org.keycloak.broker.oidc.KeycloakOIDCIdentityProvider;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.representations.JsonWebToken;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractClaimMapper extends AbstractIdentityProviderMapper {
    public static final String CLAIM = "claim";
    public static final String CLAIM_VALUE = "claim.value";

    public static Object getClaimValue(JsonWebToken token, String claim) {
        String[] split = claim.split("\\.");
        Map<String, Object> jsonObject = token.getOtherClaims();
        for (int i = 0; i < split.length; i++) {
            if (i == split.length - 1) {
                return jsonObject.get(split[i]);
            } else {
                Object val = jsonObject.get(split[i]);
                if (!(val instanceof Map)) return null;
                jsonObject = (Map<String, Object>)val;
            }
        }
        return null;
    }

    public static Object getClaimValue(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String claim = mapperModel.getConfig().get(CLAIM);
        return getClaimValue(context, claim);
    }

    public static Object getClaimValue(BrokeredIdentityContext context, String claim) {
        {  // search access token
            JsonWebToken token = (JsonWebToken)context.getContextData().get(KeycloakOIDCIdentityProvider.VALIDATED_ACCESS_TOKEN);
            if (token != null) {
                Object value = getClaimValue(token, claim);
                if (value != null) return value;
            }

        }
        {  // search ID Token
            JsonWebToken token = (JsonWebToken)context.getContextData().get(KeycloakOIDCIdentityProvider.VALIDATED_ID_TOKEN);
            if (token != null) {
                Object value = getClaimValue(token, claim);
                if (value != null) return value;
            }

        }
        return null;
    }


    protected boolean hasClaimValue(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        Object value = getClaimValue(mapperModel, context);
        String desiredValue = mapperModel.getConfig().get(CLAIM_VALUE);
        return valueEquals(desiredValue, value);
    }

    public boolean valueEquals(String desiredValue, Object value) {
        if (value instanceof String) {
            if (desiredValue.equals(value)) return true;
        } else if (value instanceof Double) {
            try {
                if (Double.valueOf(desiredValue).equals(value)) return true;
            } catch (Exception e) {

            }
        } else if (value instanceof Integer) {
            try {
                if (Integer.valueOf(desiredValue).equals(value)) return true;
            } catch (Exception e) {

            }
        } else if (value instanceof Boolean) {
            try {
                if (Boolean.valueOf(desiredValue).equals(value)) return true;
            } catch (Exception e) {

            }
        } else if (value instanceof List) {
            List list = (List)value;
            for (Object val : list) {
            	if (valueEquals(desiredValue, val)) return true;
            }
        }
        return false;
    }
}
