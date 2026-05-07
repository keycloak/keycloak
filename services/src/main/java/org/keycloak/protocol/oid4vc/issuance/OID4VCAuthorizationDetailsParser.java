package org.keycloak.protocol.oid4vc.issuance;

import java.util.List;
import java.util.Map;

import org.keycloak.protocol.oid4vc.model.ClaimsDescription;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;
import org.keycloak.util.AuthorizationDetailsParser;

import static org.keycloak.OID4VCConstants.CREDENTIAL_CONFIGURATION_ID;
import static org.keycloak.OID4VCConstants.CREDENTIAL_IDENTIFIERS;
import static org.keycloak.protocol.oid4vc.model.ClaimsDescription.MANDATORY;
import static org.keycloak.protocol.oid4vc.model.ClaimsDescription.PATH;
import static org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail.CLAIMS;
import static org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail.CREDENTIALS_OFFER_ID;

public class OID4VCAuthorizationDetailsParser implements AuthorizationDetailsParser {

    @Override
    public <T extends AuthorizationDetailsJSONRepresentation> T asSubtype(AuthorizationDetailsJSONRepresentation authzDetail, Class<T> clazz) {
        if (OID4VCAuthorizationDetail.class.equals(clazz)) {
            if (authzDetail instanceof OID4VCAuthorizationDetail) {
                return clazz.cast(authzDetail);
            } else {
                OID4VCAuthorizationDetail detail = new OID4VCAuthorizationDetail();
                fillFields(authzDetail, detail);
                return clazz.cast(detail);
            }
        } else {
            throw new IllegalArgumentException("Authorization details '" + authzDetail + "' is unsupported to be parsed to '" + clazz + "'.");
        }
    }

    private void fillFields(AuthorizationDetailsJSONRepresentation inDetail, OID4VCAuthorizationDetail outDetail) {
        outDetail.setType(inDetail.getType());
        outDetail.setLocations(inDetail.getLocations());
        outDetail.setCredentialConfigurationId((String) inDetail.getCustomData().get(CREDENTIAL_CONFIGURATION_ID));
        outDetail.setCredentialIdentifiers((List<String>) inDetail.getCustomData().get(CREDENTIAL_IDENTIFIERS));
        outDetail.setCredentialsOfferId((String) inDetail.getCustomData().get(CREDENTIALS_OFFER_ID));
        outDetail.setClaims(parseClaims((List<Map>) inDetail.getCustomData().get(CLAIMS)));
    }

    private static List<ClaimsDescription> parseClaims(List<Map> genericClaims) {
        if (genericClaims == null) {
            return null;
        }

        return genericClaims.stream()
                .map(claim -> {
                    List<Object> path = (List<Object>) claim.get(PATH);
                    Boolean mandatory = (Boolean) claim.get(MANDATORY);
                    return new ClaimsDescription(path, mandatory);
                })
                .toList();
    }
}
