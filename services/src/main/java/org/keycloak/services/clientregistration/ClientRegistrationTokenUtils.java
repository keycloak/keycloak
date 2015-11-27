package org.keycloak.services.clientregistration;

import org.keycloak.common.util.Time;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.models.ClientInitialAccessModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.Urls;
import org.keycloak.util.TokenUtil;

import javax.ws.rs.core.UriInfo;
import java.io.IOException;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientRegistrationTokenUtils {

    public static final String TYPE_INITIAL_ACCESS_TOKEN = "InitialAccessToken";
    public static final String TYPE_REGISTRATION_ACCESS_TOKEN = "RegistrationAccessToken";

    public static String updateRegistrationAccessToken(KeycloakSession session, ClientModel client) {
        return updateRegistrationAccessToken(session.getContext().getRealm(), session.getContext().getUri(), client);
    }

    public static String updateRegistrationAccessToken(RealmModel realm, UriInfo uri, ClientModel client) {
        String id = KeycloakModelUtils.generateId();
        client.setRegistrationToken(id);
        String token = createToken(realm, uri, id, TYPE_REGISTRATION_ACCESS_TOKEN, 0);
        return token;
    }

    public static String createInitialAccessToken(RealmModel realm, UriInfo uri, ClientInitialAccessModel model) {
        return createToken(realm, uri, model.getId(), TYPE_INITIAL_ACCESS_TOKEN, model.getExpiration() > 0 ? model.getTimestamp() + model.getExpiration() : 0);
    }

    public static JsonWebToken parseToken(RealmModel realm, UriInfo uri, String token) {
        JWSInput input;
        try {
            input = new JWSInput(token);
        } catch (JWSInputException e) {
            throw new ForbiddenException(e);
        }

        if (!RSAProvider.verify(input, realm.getPublicKey())) {
            throw new ForbiddenException("Invalid signature");
        }

        JsonWebToken jwt;
        try {
            jwt = input.readJsonContent(JsonWebToken.class);
        } catch (JWSInputException e) {
            throw new ForbiddenException(e);
        }

        if (!getIssuer(realm, uri).equals(jwt.getIssuer())) {
            throw new ForbiddenException("Issuer doesn't match");
        }

        if (!jwt.isActive()) {
            throw new ForbiddenException("Expired token");
        }

        if (!(TokenUtil.TOKEN_TYPE_BEARER.equals(jwt.getType()) ||
                TYPE_INITIAL_ACCESS_TOKEN.equals(jwt.getType()) ||
                TYPE_REGISTRATION_ACCESS_TOKEN.equals(jwt.getType()))) {
            throw new ForbiddenException("Invalid token type");
        }

        return jwt;
    }

    private static String createToken(RealmModel realm, UriInfo uri, String id, String type, int expiration) {
        JsonWebToken jwt = new JsonWebToken();

        String issuer = getIssuer(realm, uri);

        jwt.type(type);
        jwt.id(id);
        jwt.issuedAt(Time.currentTime());
        jwt.expiration(expiration);
        jwt.issuer(issuer);
        jwt.audience(issuer);

        String token = new JWSBuilder().jsonContent(jwt).rsa256(realm.getPrivateKey());
        return token;
    }

    private static String getIssuer(RealmModel realm, UriInfo uri) {
        return Urls.realmIssuer(uri.getBaseUri(), realm.getName());
    }

}
