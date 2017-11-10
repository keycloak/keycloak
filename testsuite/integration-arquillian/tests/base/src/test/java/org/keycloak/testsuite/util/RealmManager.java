package org.keycloak.testsuite.util;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.representations.idm.RealmRepresentation;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>.
 */
public class RealmManager {

    private static RealmResource realm;

    private RealmManager() {
    }

    public static RealmManager realm(RealmResource realm) {
        RealmManager.realm = realm;
        return new RealmManager();
    }

    public RealmManager accessCodeLifeSpan(Integer accessCodeLifespan) {
        RealmRepresentation realmRepresentation = realm.toRepresentation();
        realmRepresentation.setAccessCodeLifespan(accessCodeLifespan);
        realm.update(realmRepresentation);
        return this;
    }

    public RealmManager verifyEmail(Boolean enabled) {
        RealmRepresentation rep = realm.toRepresentation();
        rep.setVerifyEmail(enabled);
        realm.update(rep);
        return this;
    }

    public RealmManager passwordPolicy(String passwordPolicy) {
        RealmRepresentation rep = realm.toRepresentation();
        rep.setPasswordPolicy(passwordPolicy);
        realm.update(rep);
        return this;
    }

    public RealmManager revokeRefreshToken(boolean enable) {
        RealmRepresentation rep = realm.toRepresentation();
        rep.setRevokeRefreshToken(enable);
        realm.update(rep);
        return this;
    }

    public RealmManager refreshTokenMaxReuse(int refreshTokenMaxReuse) {
        RealmRepresentation rep = realm.toRepresentation();
        rep.setRefreshTokenMaxReuse(refreshTokenMaxReuse);
        realm.update(rep);
        return this;
    }

    public void generateKeys() {
        RealmRepresentation rep = realm.toRepresentation();

        KeyPair keyPair;
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            keyPair = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        rep.setPrivateKey(Base64.encodeBytes(keyPair.getPrivate().getEncoded()));
        rep.setPublicKey(Base64.encodeBytes(keyPair.getPublic().getEncoded()));
        X509Certificate certificate;
        try {
            certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, rep.getId());
            rep.setCertificate(Base64.encodeBytes(certificate.getEncoded()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        rep.setCodeSecret(org.keycloak.models.utils.KeycloakModelUtils.generateCodeSecret());
        realm.update(rep);
    }

    public void keyPair(String privateKey, String publicKey) {
        RealmRepresentation rep = realm.toRepresentation();
        rep.setPrivateKey(privateKey);
        rep.setPublicKey(publicKey);
        realm.update(rep);
    }

    public void ssoSessionMaxLifespan(int ssoSessionMaxLifespan) {
        RealmRepresentation rep = realm.toRepresentation();
        rep.setSsoSessionMaxLifespan(ssoSessionMaxLifespan);
        realm.update(rep);
    }

    public void sslRequired(String sslRequired) {
        RealmRepresentation rep = realm.toRepresentation();
        rep.setSslRequired(sslRequired);
        realm.update(rep);
    }

    public void accessTokenLifespan(int accessTokenLifespan) {
        RealmRepresentation rep = realm.toRepresentation();
        rep.setAccessTokenLifespan(accessTokenLifespan);
        realm.update(rep);
    }

    public RealmManager ssoSessionIdleTimeout(int ssoSessionIdleTimeout) {
        RealmRepresentation rep = realm.toRepresentation();
        rep.setSsoSessionIdleTimeout(ssoSessionIdleTimeout);
        realm.update(rep);
        return this;
    }
}