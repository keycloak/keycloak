package org.keycloak.services.validation;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.utils.PairwiseSubMapperValidator;
import org.keycloak.protocol.oidc.utils.SubjectType;
import org.keycloak.representations.idm.ClientRepresentation;

import java.util.HashSet;
import java.util.Set;


/**
 * @author <a href="mailto:martin.hardselius@gmail.com">Martin Hardselius</a>
 */
public class PairwiseClientValidator {

    public static boolean validate(KeycloakSession session, ClientRepresentation client, ValidationMessages messages) {
        OIDCAdvancedConfigWrapper configWrapper = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);
        if (configWrapper.getSubjectType().equals(SubjectType.PAIRWISE)) {
            String sectorIdentifierUri = configWrapper.getSectorIdentifierUri();
            String rootUrl = client.getRootUrl();
            Set<String> redirectUris = new HashSet<>();
            if (client.getRedirectUris() != null) redirectUris.addAll(client.getRedirectUris());
            return validate(session, rootUrl, redirectUris, sectorIdentifierUri, messages);
        }
        return true;
    }

    public static boolean validate(KeycloakSession session, String rootUrl, Set<String> redirectUris, String sectorIdentifierUri, ValidationMessages messages) {
        try {
            PairwiseSubMapperValidator.validate(session, rootUrl, redirectUris, sectorIdentifierUri);
        } catch (ProtocolMapperConfigException e) {
            messages.add(e.getMessage(), e.getMessageKey());
            return false;
        }
        return true;
    }

}
