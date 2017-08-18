package org.keycloak.services.validation;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper;
import org.keycloak.protocol.oidc.utils.PairwiseSubMapperUtils;
import org.keycloak.protocol.oidc.utils.PairwiseSubMapperValidator;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * @author <a href="mailto:martin.hardselius@gmail.com">Martin Hardselius</a>
 */
public class PairwiseClientValidator {

    public static boolean validate(KeycloakSession session, ClientRepresentation client, ValidationMessages messages) {
        String rootUrl = client.getRootUrl();
        Set<String> redirectUris = new HashSet<>();
        boolean valid = true;

        List<ProtocolMapperRepresentation> foundPairwiseMappers = PairwiseSubMapperUtils.getPairwiseSubMappers(client);

        for (ProtocolMapperRepresentation foundPairwise : foundPairwiseMappers) {
            String sectorIdentifierUri = PairwiseSubMapperHelper.getSectorIdentifierUri(foundPairwise);
            if (client.getRedirectUris() != null) redirectUris.addAll(client.getRedirectUris());
            valid = valid && validate(session, rootUrl, redirectUris, sectorIdentifierUri, messages);
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
