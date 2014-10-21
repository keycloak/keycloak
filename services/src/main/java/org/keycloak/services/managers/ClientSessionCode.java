package org.keycloak.services.managers;

import org.keycloak.OAuthErrorException;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.util.Base64Url;
import org.keycloak.util.Time;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.Signature;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientSessionCode {

    private final RealmModel realm;
    private final ClientSessionModel clientSession;

    public ClientSessionCode(RealmModel realm, ClientSessionModel clientSession) {
        this.realm = realm;
        this.clientSession = clientSession;
    }

    public static ClientSessionCode parse(String code, KeycloakSession session) {
        try {
            String[] parts = code.split("\\.");
            String id = new String(Base64Url.decode(parts[1]));

            ClientSessionModel clientSession = session.sessions().getClientSession(id);
            if (clientSession == null) {
                return null;
            }

            String hash = createSignatureHash(clientSession.getRealm(), clientSession);
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
            String id = new String(Base64Url.decode(parts[1]));

            ClientSessionModel clientSession = session.sessions().getClientSession(realm, id);
            if (clientSession == null) {
                return null;
            }

            String hash = createSignatureHash(realm, clientSession);
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

    public boolean isValid(RequiredAction requiredAction) {
        return isValid(convertToAction(requiredAction));
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
        String hash = createSignatureHash(realm, clientSession);

        StringBuilder sb = new StringBuilder();
        sb.append(hash);
        sb.append(".");
        sb.append(Base64Url.encode(clientSession.getId().getBytes()));

        return sb.toString();
    }

    private static String createSignatureHash(RealmModel realm, ClientSessionModel clientSession) {
        try {
            Signature signature = Signature.getInstance(RSAProvider.getJavaAlgorithm(Algorithm.RS256));
            signature.initSign(realm.getPrivateKey());
            signature.update(clientSession.getId().getBytes());
            signature.update(ByteBuffer.allocate(4).putInt(clientSession.getTimestamp()));
            if (clientSession.getAction() != null) {
                signature.update(clientSession.getAction().toString().getBytes());
            }
            byte[] sign = signature.sign();

            MessageDigest digest = MessageDigest.getInstance("sha-1");
            digest.update(sign);
            return Base64Url.encode(digest.digest());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
