package org.keycloak.vault;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Optional;

import jakarta.annotation.Nonnull;

import org.jboss.logging.Logger;

public class FilesKeystoreVaultProvider extends AbstractVaultProvider {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private final Path keystorePath;
    private final String keystorePass;
    private final String keystoreType;

    /**
     * Creates a new {@link FilesKeystoreVaultProvider}.
     *
     * @param keystorePath A path to a vault. Can not be null.
     * @param keystorePass A password to a vault. Can not be null.
     * @param keystoreType Specifies a type of keystore. Can not be null. Default value is PKCS12.
     * @param realmName    A realm name. Can not be null.
     */
    public FilesKeystoreVaultProvider(@Nonnull Path keystorePath, @Nonnull String keystorePass, @Nonnull String keystoreType,
                                      @Nonnull String realmName, @Nonnull List<VaultKeyResolver> resolvers) {
        super(realmName, resolvers);
        this.keystorePath = keystorePath;
        this.keystorePass = keystorePass;
        this.keystoreType = keystoreType;
        logger.debugf("KeystoreVaultProvider will operate in %s directory", keystorePath.toAbsolutePath());
    }

    @Override
    protected VaultRawSecret obtainSecretInternal(String alias) {
        KeyStore ks;
        Key key;
        try {
            if (!Files.exists(keystorePath.toRealPath())) {
                throw new VaultNotFoundException("The keystore file for Keycloak Vault was not found");
            }
            ks = KeyStore.getInstance(keystoreType);
            ks.load(Files.newInputStream(keystorePath.toRealPath()), keystorePass.toCharArray());
            key = ks.getKey(alias, keystorePass.toCharArray());
            if (key == null) {
                logger.warnf("Cannot find secret %s in %s", alias, keystorePath);
                return DefaultVaultRawSecret.forBuffer(Optional.empty());
            }
        } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException | UnrecoverableEntryException e) {
            throw new RuntimeException(e);
        }
        return DefaultVaultRawSecret.forBuffer(Optional.of(ByteBuffer.wrap(new String(key.getEncoded()).getBytes())));
    }

    @Override
    public void close() {

    }
}
