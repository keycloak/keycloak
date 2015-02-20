package org.keycloak.protocol.oidc;

import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessToken;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Mappings user data to an ID Token claim.  Source can be from UserModel.getAttributes(), a get method on UserModel, UserSession.getNote
 * or ClientSession.getNote.  Claim can be a full qualified nested object name, i.e. "address.country".  This will create a nested
 * json object within the toke claim.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCAttributeToTokenMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenTransformer {
    @Override
    public String getId() {
        return "oidc-attribute-claim-mapper";
    }

    @Override
    public String getDisplayType() {
        return "Attribute Claim Mapper";
    }

    @Override
    public AccessToken transformToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                      UserSessionModel userSession, ClientSessionModel clientSession) {
        String attributeValue = null;
        UserModel user = userSession.getUser();
        switch (mappingModel.getSource()) {
            case USER_ATTRIBUTE:
                attributeValue = user.getAttribute(mappingModel.getSourceAttribute());
                break;
            case USER_SESSION_NOTE:
                attributeValue = userSession.getNote(mappingModel.getSourceAttribute());
                break;
            case CLIENT_SESSION_NOTE:
                attributeValue = clientSession.getNote(mappingModel.getSourceAttribute());
                break;
            case USER_MODEL:
                attributeValue = getUserModelValue(user, mappingModel);
                break;
        }
        if (attributeValue == null) return token;
        String[] split = mappingModel.getProtocolClaim().split(".");
        Map<String, Object> jsonObject = token.getOtherClaims();
        for (int i = 0; i < split.length; i++) {
            if (i == split.length - 1) {
                jsonObject.put(split[i], attributeValue);
            } else {
                Map<String, Object> nested = (Map<String, Object>)jsonObject.get(split[i]);
                if (nested == null) {
                    nested = new HashMap<String, Object>();
                    jsonObject.put(split[i], nested);
                    jsonObject = nested;
                }
            }
        }
        return token;
    }

    protected String getUserModelValue(UserModel user, ProtocolMapperModel model) {
        String sourceAttribute = model.getSourceAttribute();
        if (sourceAttribute == null) return null;

        String methodName = "get" + Character.toUpperCase(sourceAttribute.charAt(0)) + sourceAttribute.substring(1);
        try {
            Method method = UserModel.class.getMethod(methodName);
            Object val = method.invoke(user);
            if (val != null) return val.toString();
        } catch (Exception ignore) {

        }
        return null;
    }
}
