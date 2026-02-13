package org.keycloak.protocol.oid4vc.utils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.model.DisplayObject;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.logging.Logger;

import static org.keycloak.constants.OID4VCIConstants.OID4VC_PROTOCOL;
import static org.keycloak.models.oid4vci.CredentialScopeModel.CONFIGURATION_ID;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_DISPLAY;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint.CREDENTIAL_OFFER_PATH;

public class OID4VCUtil {

    private static final Logger logger = Logger.getLogger(OID4VCUtil.class);

    private OID4VCUtil() {
    }

    /**
     * @param session Keycloak session
     * @param nonce nonce, which is part of the credential offer URI
     * @return Credential offer as URI, which can be shared with the wallet
     */
    public static String getOfferAsUri(KeycloakSession session, String nonce) {
        String encodedOfferUri = URLEncoder.encode(OID4VCIssuerWellKnownProvider.getIssuer(session.getContext()) + "/protocol/" + OID4VCLoginProtocolFactory.PROTOCOL_ID + "/" + CREDENTIAL_OFFER_PATH + "/" + nonce, StandardCharsets.UTF_8);
        return "openid-credential-offer://?credential_offer_uri=" + encodedOfferUri;
    }


    /**
     * Return display name of the credential according to preferred locale of current user and according to "vc.display" attribute specified for current OID4VCI client
     * scope. Will fallback to client scope name if client scope does not contain "vc.display" or if "vc.display" is incorrectly formatted
     *
     * @param session Keycloak session
     * @param user user
     * @param clientScope OID4VCI client scope
     * @return user-friendly name of the VC localized in the preference of the current user
     */
    public static String getCredentialDisplayName(KeycloakSession session, UserModel user, ClientScopeModel clientScope) {
        String display = clientScope.getAttribute(VC_DISPLAY);
        if (StringUtil.isNotBlank(display)) {
            try {
                List<DisplayObject> displayDatas = JsonSerialization.readValue(display, new TypeReference<>() {});
                String language = session.getContext().resolveLocale(user).getLanguage();
                String languageCountry = language + "-" + language.toUpperCase();
                for (DisplayObject displayData : displayDatas) {
                    if (language.equals(displayData.getLocale()) || languageCountry.equals(displayData.getLocale())) {
                        return displayData.getName();
                    }
                }
            } catch (IOException ioe) {
                logger.warnf("Incorrect vc.display for client scope '%s'", clientScope.getName());
            }
        }

        // Fallback
        display = clientScope.getAttribute(CONFIGURATION_ID);
        return StringUtil.isNotBlank(display) ? display :  clientScope.getName();
    }

    /**
     * Find OID4VCI client scope by credential config ID
     *
     * @param session Keycloak session
     * @param realmModel realm
     * @param credentialConfigId credential configuration ID
     * @return Found OID4VCI client scope
     */
    public static ClientScopeModel getClientScopeByCredentialConfigId(KeycloakSession session, RealmModel realmModel, String credentialConfigId) {
        if (StringUtil.isBlank(credentialConfigId)) {
            return null;
        }

        List<ClientScopeModel> clientScopes = session.clientScopes()
                .getClientScopesByProtocol(realmModel, OID4VC_PROTOCOL)
                .filter(it -> credentialConfigId.equals(it.getAttribute(CredentialScopeModel.CONFIGURATION_ID)))
                .toList();
        if (clientScopes.size() > 1) {
            List<String> clientScopeNames = clientScopes.stream()
                    .map(ClientScopeModel::getName)
                    .toList();
            logger.warnf("Multiple client scopes find with credential config ID '%s' in the realm '%s'. Please make sure that credential-config-id is unique across client scopes. Found client scopes: %s",
                    credentialConfigId, realmModel.getName(), clientScopeNames);
            return null;
        } else if (clientScopes.isEmpty()) {
            logger.warnf("No client scopes find with credential config ID '%s' in the realm '%s'",
                    credentialConfigId, realmModel.getName());
            return null;
        } else {
            return clientScopes.get(0);
        }
    }
}
