package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Mappings UserSessionModel.note to an ID Token claim.  Token claim name can be a full qualified nested object name,
 * i.e. "address.country".  This will create a nested
 * json object within the toke claim.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCUserSessionNoteMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper {
    private static final List<ConfigProperty> configProperties = new ArrayList<ConfigProperty>();
    public static final String USER_SESSION_NOTE = "UserSession Note";

    static {
        ConfigProperty property;
        property = new ConfigProperty();
        property.setName(USER_SESSION_NOTE);
        property.setLabel("UserSession Note");
        property.setHelpText("Name of the note to map in the UserSessionModel");
        configProperties.add(property);
        property = new ConfigProperty();
        property.setName(AttributeMapperHelper.TOKEN_CLAIM_NAME);
        property.setLabel(AttributeMapperHelper.TOKEN_CLAIM_NAME);
        property.setHelpText("Name of the claim to insert into the token.  This can be a fully qualified name like 'address.street'.  In this case, a nested json object will be created.");
        configProperties.add(property);

    }

    public List<ConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return "oidc-user-session-note-mapper";
    }

    @Override
    public String getDisplayType() {
        return "UserSession Note";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map a temporary note that is attached to the UserSession to a token claim.";
    }

    @Override
    public AccessToken transformToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                      UserSessionModel userSession, ClientSessionModel clientSession) {
        String note = mappingModel.getConfig().get(USER_SESSION_NOTE);
        String noteValue = userSession.getNote(note);
        AttributeMapperHelper.mapClaim(token, mappingModel, noteValue);
        return token;
    }

}
