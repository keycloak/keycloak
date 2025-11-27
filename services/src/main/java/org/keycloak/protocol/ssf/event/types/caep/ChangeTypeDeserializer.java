package org.keycloak.protocol.ssf.event.types.caep;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * The official change types are create, revoke, update, deleted, but some legacy implementations use created etc.
 * See: https://openid.net/specs/openid-caep-specification-1_0.html#rfc.section.3.3.1
 */
public class ChangeTypeDeserializer extends JsonDeserializer<CredentialChange.ChangeType> {

    private static final Map<String, CredentialChange.ChangeType> CHANGE_TYPE_MAP = new HashMap<>();

    static {
        // some existing SSF transmitters use (older) non standard change type identifiers.
        CHANGE_TYPE_MAP.put("create", CredentialChange.ChangeType.CREATE);
        CHANGE_TYPE_MAP.put("created", CredentialChange.ChangeType.CREATE); // Handle non-standard form

        CHANGE_TYPE_MAP.put("revoke", CredentialChange.ChangeType.REVOKE);
        CHANGE_TYPE_MAP.put("revoked", CredentialChange.ChangeType.REVOKE); // Handle non-standard form

        CHANGE_TYPE_MAP.put("update", CredentialChange.ChangeType.UPDATE);
        CHANGE_TYPE_MAP.put("updated", CredentialChange.ChangeType.UPDATE); // Handle non-standard form

        CHANGE_TYPE_MAP.put("delete", CredentialChange.ChangeType.DELETE);
        CHANGE_TYPE_MAP.put("deleted", CredentialChange.ChangeType.DELETE); // Handle non-standard form
    }

    @Override
    public CredentialChange.ChangeType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText().toLowerCase(); // Normalize input
        CredentialChange.ChangeType changeType = CHANGE_TYPE_MAP.get(value);

        if (changeType == null) {
            throw new IOException("Unknown changeType value: " + value);
        }

        return changeType;
    }
}
