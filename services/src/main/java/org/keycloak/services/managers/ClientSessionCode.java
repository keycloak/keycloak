package org.keycloak.services.managers;

import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.util.Base64Url;
import org.keycloak.util.Time;

import javax.crypto.Mac;
import java.security.Key;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientSessionCode {

    public static final String ACTION_KEY = "action_key";

    private static final byte[] HASH_SEPERATOR = "//".getBytes();

    private final RealmModel realm;
    private final ClientSessionModel clientSession;

    public ClientSessionCode(RealmModel realm, ClientSessionModel clientSession) {
        this.realm = realm;
        this.clientSession = clientSession;
    }

    public static ClientSessionCode parse(String code, KeycloakSession session) {
        try {
            String[] parts = code.split("\\.");
            String id = parts[1];

            ClientSessionModel clientSession = session.sessions().getClientSession(id);
            if (clientSession == null) {
                return null;
            }

            String hash = createHash(clientSession.getRealm(), clientSession);
            if (!hash.equals(parts[0])) {
                return null;
            }

            return new ClientSessionCode(clientSession.getRealm(), clientSession);
        } catch (RuntimeException e) {
            return null;
        }
    }


    public static ClientSessionCode parse(String code, KeycloakSession session, RealmModel realm) {
        try {
            String[] parts = code.split("\\.");
            String id = parts[1];

            ClientSessionModel clientSession = session.sessions().getClientSession(realm, id);
            if (clientSession == null) {
                return null;
            }

            String hash = createHash(realm, clientSession);
            if (!hash.equals(parts[0])) {
                return null;
            }

            return new ClientSessionCode(realm, clientSession);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public ClientSessionModel getClientSession() {
        return clientSession;
    }

    public boolean isValid(ClientSessionModel.Action requestedAction) {
        ClientSessionModel.Action action = clientSession.getAction();
        if (action == null) {
            return false;
        }

        int timestamp = clientSession.getTimestamp();

        if (!action.equals(requestedAction)) {
            return false;
        }

        int lifespan = action.equals(ClientSessionModel.Action.CODE_TO_TOKEN) ? realm.getAccessCodeLifespan() : realm.getAccessCodeLifespanUserAction();
        return timestamp + lifespan > Time.currentTime();
    }

    public Set<RoleModel> getRequestedRoles() {
        Set<RoleModel> requestedRoles = new HashSet<RoleModel>();
        for (String roleId : clientSession.getRoles()) {
            RoleModel role = realm.getRoleById(roleId);
            if (role != null) {
                requestedRoles.add(realm.getRoleById(roleId));
            }
        }
        return requestedRoles;
    }

    public void setAction(ClientSessionModel.Action action) {
        clientSession.setAction(action);
        clientSession.setNote(ACTION_KEY, UUID.randomUUID().toString());
        clientSession.setTimestamp(Time.currentTime());
    }

    public void setRequiredAction(RequiredAction requiredAction) {
        setAction(convertToAction(requiredAction));
    }

    private ClientSessionModel.Action convertToAction(RequiredAction requiredAction) {
        switch (requiredAction) {
            case CONFIGURE_TOTP:
                return ClientSessionModel.Action.CONFIGURE_TOTP;
            case UPDATE_PASSWORD:
                return ClientSessionModel.Action.UPDATE_PASSWORD;
            case UPDATE_PROFILE:
                return ClientSessionModel.Action.UPDATE_PROFILE;
            case VERIFY_EMAIL:
                return ClientSessionModel.Action.VERIFY_EMAIL;
            default:
                throw new IllegalArgumentException("Unknown required action " + requiredAction);
        }
    }

    public String getCode() {
        return generateCode(realm, clientSession);
    }

    private static String generateCode(RealmModel realm, ClientSessionModel clientSession) {
        String hash = createHash(realm, clientSession);

        StringBuilder sb = new StringBuilder();
        sb.append(hash);
        sb.append(".");
        sb.append(clientSession.getId());

        return sb.toString();
    }

    private static String createHash(RealmModel realm, ClientSessionModel clientSession) {
        try {
            Key codeSecretKey = realm.getCodeSecretKey();
            Mac mac = Mac.getInstance(codeSecretKey.getAlgorithm());
            mac.init(codeSecretKey);
            mac.update(clientSession.getId().getBytes());
            mac.update(HASH_SEPERATOR);
            mac.update(clientSession.getNote(ACTION_KEY).getBytes());
            return Base64Url.encode(mac.doFinal());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
