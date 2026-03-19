package org.keycloak.protocol.oid4vc.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.model.DisplayObject;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.util.Strings;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

import static org.keycloak.constants.OID4VCIConstants.OID4VC_PROTOCOL;

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

    /**
     * Get the list of credential scopes associated by the given and requested by the given authorization request
     */
    public static List<CredentialScopeModel> getCredentialScopesForAuthorization(ClientModel client, AuthorizationEndpointRequest request) {

        List<String> requestScopes = Optional.ofNullable(request.getScope())
                .map(it -> it.split("\\s"))
                .map(Arrays::asList)
                .orElse(List.of());

        // Get the list of requested credential scopes that are associated with this client
        //
        Map<String, ClientScopeModel> clientScopes = client.getClientScopes(false);
        List<CredentialScopeModel> credScopes = requestScopes.stream()
                .filter(clientScopes::containsKey)
                .map(clientScopes::get)
                .filter(it -> OID4VC_PROTOCOL.equals(it.getProtocol()))
                .map(CredentialScopeModel::new)
                .toList();

        return credScopes;
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
