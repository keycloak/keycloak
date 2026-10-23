package org.keycloak.tests.providers.oid4vc;

import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oid4vc.issuance.VCIssuanceContext;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCStaticClaimMapper;

/** Test mapper that resolves an external record using the issuance context. */
public class ClaimsReferenceMapper extends OID4VCStaticClaimMapper {

    public static final String ID = "test-oid4vc-claims-reference";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ProtocolMapper create(KeycloakSession session) {
        return new ClaimsReferenceMapper();
    }

    @Override
    public void setClaim(Map<String, Object> claims, UserSessionModel userSession, VCIssuanceContext context) {
        String reference = context.getClaimsRef();
        // A missing context or invocation of the legacy method must not accidentally satisfy the test.
        String value = reference == null ? "no-reference" : Map.of(
                "booking/42?case=one & two", "confirmed",
                "booking-43", "cancelled").get(reference);
        claims.put(getClaimName(), value);
    }
}
