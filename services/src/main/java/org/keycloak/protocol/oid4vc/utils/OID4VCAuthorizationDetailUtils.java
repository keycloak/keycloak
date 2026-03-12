package org.keycloak.protocol.oid4vc.utils;

import java.util.List;
import java.util.Optional;

import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferState;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.util.Strings;

import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_CONFIGURATION_ID;

public class OID4VCAuthorizationDetailUtils {

    public static OID4VCAuthorizationDetail buildOID4VCAuthorizationDetail(CredentialScopeModel credScopeModel, CredentialOfferState offerState) {

        // Read the credential_identifiers from the client scope configuration
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);

        String credConfigId = Optional.ofNullable(credScopeModel.getCredentialConfigurationId())
                .orElseThrow(() -> new IllegalStateException("No " + VC_CONFIGURATION_ID + " in client scope: " + credScopeModel.getName()));
        authDetail.setCredentialConfigurationId(credConfigId);

        String credIdentifier = credScopeModel.getCredentialIdentifier();
        if (!Strings.isEmpty(credIdentifier)) {
            authDetail.setCredentialIdentifiers(List.of(credIdentifier));
        }

        if (offerState != null) {
            authDetail.setCredentialsOfferId(offerState.getCredentialsOfferId());
        }

        return authDetail;
    }
}
