package org.keycloak.models.utils;

import org.bouncycastle.openssl.PEMWriter;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClaimMask;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.util.CertificateUtils;
import org.keycloak.util.PemUtils;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.StringWriter;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.UUID;

/**
 * Set of helper methods, which are useful in various model implementations.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public final class KeycloakModelUtils {

    private KeycloakModelUtils() {
    }

    public static String generateId() {
        return UUID.randomUUID().toString();
    }

    public static PublicKey getPublicKey(String publicKeyPem) {
        if (publicKeyPem != null) {
            try {
                return PemUtils.decodePublicKey(publicKeyPem);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }

    public static X509Certificate getCertificate(String cert) {
        if (cert != null) {
            try {
                return PemUtils.decodeCertificate(cert);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }


    public static PrivateKey getPrivateKey(String privateKeyPem) {
        if (privateKeyPem != null) {
            try {
                return PemUtils.decodePrivateKey(privateKeyPem);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public static Key getSecretKey(String secret) {
        return secret != null ? new SecretKeySpec(secret.getBytes(), "HmacSHA256") : null;
    }

    public static String getPemFromKey(Key key) {
        StringWriter writer = new StringWriter();
        PEMWriter pemWriter = new PEMWriter(writer);
        try {
            pemWriter.writeObject(key);
            pemWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String s = writer.toString();
        return PemUtils.removeBeginEnd(s);
    }

    public static String getPemFromCertificate(X509Certificate certificate) {
        StringWriter writer = new StringWriter();
        PEMWriter pemWriter = new PEMWriter(writer);
        try {
            pemWriter.writeObject(certificate);
            pemWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String s = writer.toString();
        return PemUtils.removeBeginEnd(s);
    }

    public static void generateRealmKeys(RealmModel realm) {
        KeyPair keyPair = null;
        try {
            keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        realm.setPrivateKey(keyPair.getPrivate());
        realm.setPublicKey(keyPair.getPublic());
        X509Certificate certificate = null;
        try {
            certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, realm.getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        realm.setCertificate(certificate);

        realm.setCodeSecret(generateCodeSecret());
    }

    public static void generateRealmCertificate(RealmModel realm) {
        X509Certificate certificate = null;
        try {
            certificate = CertificateUtils.generateV1SelfSignedCertificate(new KeyPair(realm.getPublicKey(), realm.getPrivateKey()), realm.getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        realm.setCertificate(certificate);
    }

    public static void generateClientKeyPairCertificate(ClientModel client) {
        String subject = client.getClientId();
        KeyPair keyPair = null;
        try {
            keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        X509Certificate certificate = null;
        try {
            certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, subject);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String privateKeyPem = KeycloakModelUtils.getPemFromKey(keyPair.getPrivate());
        String publicKeyPem = KeycloakModelUtils.getPemFromKey(keyPair.getPublic());
        String certPem = KeycloakModelUtils.getPemFromCertificate(certificate);

        client.setAttribute(ClientModel.PRIVATE_KEY, privateKeyPem);
        client.setAttribute(ClientModel.PUBLIC_KEY, publicKeyPem);
        client.setAttribute(ClientModel.X509CERTIFICATE, certPem);

    }

    public static UserCredentialModel generateSecret(ClientModel app) {
        UserCredentialModel secret = UserCredentialModel.generateSecret();
        app.setSecret(secret.getValue());
        return secret;
    }

    public static String generateCodeSecret() {
        return UUID.randomUUID().toString();
    }

    public static ApplicationModel createApplication(RealmModel realm, String name) {
        ApplicationModel app = realm.addApplication(name);
        generateSecret(app);
        app.setFullScopeAllowed(true);
        app.setAllowedClaimsMask(ClaimMask.ALL);

        return app;
    }

    /**
     * Deep search if given role is descendant of composite role
     *
     * @param role role to check
     * @param composite composite role
     * @param visited set of already visited roles (used for recursion)
     * @return true if "role" is descendant of "composite"
     */
    public static boolean searchFor(RoleModel role, RoleModel composite, Set<RoleModel> visited) {
        if (visited.contains(composite)) return false;
        visited.add(composite);
        Set<RoleModel> composites = composite.getComposites();
        if (composites.contains(role)) return true;
        for (RoleModel contained : composites) {
            if (!contained.isComposite()) continue;
            if (searchFor(role, contained, visited)) return true;
        }
        return false;
    }

    /**
     * Try to find user by given username. If it fails, then fallback to find him by email
     *
     * @param realm realm
     * @param username username or email of user
     * @return found user
     */
    public static UserModel findUserByNameOrEmail(KeycloakSession session, RealmModel realm, String username) {
        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null && username.contains("@")) {
            user =  session.users().getUserByEmail(username, realm);
        }
        return user;
    }

    /**
     * Wrap given runnable job into KeycloakTransaction.
     *
     * @param factory
     * @param task
     */
    public static void runJobInTransaction(KeycloakSessionFactory factory, KeycloakSessionTask task) {
        KeycloakSession session = factory.create();
        KeycloakTransaction tx = session.getTransaction();
        try {
            tx.begin();
            task.run(session);

            if (tx.isActive()) {
                if (tx.getRollbackOnly()) {
                    tx.rollback();
                } else {
                    tx.commit();
                }
            }
        } catch (RuntimeException re) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw re;
        } finally {
            session.close();
        }
    }

    public static String getMasterRealmAdminApplicationName(RealmModel realm) {
        return realm.getName() + "-realm";
    }
}
