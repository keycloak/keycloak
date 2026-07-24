package org.keycloak.protocol.oid4vc.model;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import static org.keycloak.protocol.oid4vc.model.AuthorizationCodeGrant.AUTH_CODE_GRANT_TYPE;
import static org.keycloak.protocol.oid4vc.model.PreAuthorizedCodeGrant.PRE_AUTH_GRANT_TYPE;

public final class CredentialOfferGrantsDeserializer extends JsonDeserializer<Map<String, CredentialOfferGrant>> {

    @Override
    public Map<String, CredentialOfferGrant> deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

        Map<String, CredentialOfferGrant> grants = new LinkedHashMap<>();
        var mapper = (ObjectMapper) p.getCodec();
        var node = mapper.readTree(p);

        var fields = node.fieldNames();
        while (fields.hasNext()) {
            var grantType = fields.next();
            var valueNode = node.get(grantType);

            Class<? extends CredentialOfferGrant> target =
                    switch (grantType) {
                        case AUTH_CODE_GRANT_TYPE -> AuthorizationCodeGrant.class;
                        case PRE_AUTH_GRANT_TYPE -> PreAuthorizedCodeGrant.class;
                        default -> throw new InvalidFormatException(
                                p, "Unknown grant type key: " + grantType, grantType, CredentialOfferGrant.class);
                    };

            CredentialOfferGrant grant = mapper.treeToValue(valueNode, target);
            grants.put(grantType, grant);
        }
        return grants;
    }
}
