package org.keycloak.protocol.oid4vc.utils;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.model.DisplayObject;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.util.Strings;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;
import static org.keycloak.constants.OID4VCIConstants.OID4VC_PROTOCOL;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_CONFIGURATION_ID;

public class CredentialScopeModelUtils {

    private static final Logger log = Logger.getLogger(CredentialScopeModelUtils.class);

    // Hide ctor
    private CredentialScopeModelUtils() {}

    public static CredentialScopeModel findCredentialScopeModelByConfigurationId(RealmModel realmModel, Supplier<Stream<ClientScopeModel>> supplier, String credConfigId) {
        if (Strings.isEmpty(credConfigId)) {
            return null;
        }
        List<CredentialScopeModel> credScopes = supplier.get()
                .filter(it -> it.getProtocol().equals(OID4VC_PROTOCOL))
                .map(CredentialScopeModel::new)
                .filter(it -> credConfigId.equals(it.getCredentialConfigurationId()))
                .toList();
        if (credScopes.size() > 1) {
            List<String> clientScopeNames = credScopes.stream().map(ClientScopeModel::getName).toList();
            log.warnf("Multiple client scopes found for credential configuration '%s' in realm '%s': %s",
                    credConfigId, realmModel.getName(), clientScopeNames);
            return null;
        } else if (credScopes.isEmpty()) {
            log.warnf("No client scopes found for credential configuration '%s' in realm '%s'",
                    credConfigId, realmModel.getName());
            return null;
        } else {
            return credScopes.get(0);
        }
    }

    public static CredentialScopeModel findCredentialScopeModelByName(RealmModel realmModel, Supplier<Stream<ClientScopeModel>> supplier, String scope) {
        if (Strings.isEmpty(scope)) {
            return null;
        }
        List<CredentialScopeModel> credScopes =  supplier.get()
                .filter(it -> it.getProtocol().equals(OID4VC_PROTOCOL))
                .map(CredentialScopeModel::new)
                .filter(it -> scope.equals(it.getScope()))
                .toList();
        if (credScopes.size() > 1) {
            List<String> clientScopeNames = credScopes.stream().map(ClientScopeModel::getName).toList();
            log.warnf("Multiple client scopes found for scope '%s' in realm '%s': %s",
                    scope, realmModel.getName(), clientScopeNames);
            return null;
        }
        return !credScopes.isEmpty() ? credScopes.get(0) : null;
    }

    public static OID4VCAuthorizationDetail buildOID4VCAuthorizationDetail(CredentialScopeModel credScope, String credOffersId) {

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setCredentialsOfferId(credOffersId);
        authDetail.setType(OPENID_CREDENTIAL);

        String credConfigId = Optional.ofNullable(credScope.getCredentialConfigurationId())
                .orElseThrow(() -> new IllegalStateException("No " + VC_CONFIGURATION_ID + " in client scope: " + credScope.getName()));

        authDetail.setCredentialConfigurationId(credConfigId);

        String credIdentifier = credScope.getCredentialIdentifier();
        if (!Strings.isEmpty(credIdentifier)) {
            authDetail.setCredentialIdentifiers(List.of(credIdentifier));
        }

        return authDetail;
    }

    /**
     * Return display name of the credential according to preferred locale of current user and according to "vc.display" attribute specified for current OID4VCI client
     * scope. Will fallback to client scope name if client scope does not contain "vc.display" or if "vc.display" is incorrectly formatted
     *
     * @param session Keycloak session
     * @param user user
     * @param credScope OID4VCI client scope
     * @return user-friendly name of the VC localized in the preference of the current user
     */
    public static String getCredentialDisplayName(KeycloakSession session, UserModel user, CredentialScopeModel credScope) {
        List<DisplayObject> displayDatas = DisplayObject.parse(credScope);
        if (displayDatas != null) {
            String language = session.getContext().resolveLocale(user).getLanguage();
            String languageCountry = language + "-" + language.toUpperCase();
            for (DisplayObject displayData : displayDatas) {
                if (language.equals(displayData.getLocale()) || languageCountry.equals(displayData.getLocale())) {
                    return displayData.getName();
                }
            }
        }

        // Fallback
        String display = credScope.getCredentialConfigurationId();
        return StringUtil.isNotBlank(display) ? display :  credScope.getName();
    }
}
