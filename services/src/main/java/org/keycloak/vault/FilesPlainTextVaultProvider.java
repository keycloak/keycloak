package org.keycloak.vault;

import org.jboss.logging.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.Optional;

/**
 * A text-based vault provider, which stores each secret in a separate file. The file name needs to match a
 * vault secret id (or a key for short). A typical vault directory layout looks like this:
 * <pre>
 *     ${VAULT}/realma__key1 (contains secret for key 1)
 *     ${VAULT}/realma__key2 (contains secret for key 2)
 *     etc...
 * </pre>
 * Note, that each key needs is prefixed by realm name. This kind of layout is used by Kubernetes by default
 * (when mounting a volume into the pod).
 *
 * See https://kubernetes.io/docs/concepts/configuration/secret/
 * See https://github.com/keycloak/keycloak-community/blob/master/design/secure-credentials-store.md#plain-text-file-per-secret-kubernetes--openshift
 *
 * @author Sebastian Łaskawiec
 */
public class FilesPlainTextVaultProvider implements VaultProvider {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private final Path vaultPath;
    private final String realmName;

    /**
     * Creates a new {@link FilesPlainTextVaultProvider}.
     *
     * @param path A path to a vault. Can not be null.
     * @param realmName A realm name. Can not be null.
     */
    public FilesPlainTextVaultProvider(@Nonnull Path path, @Nonnull String realmName) {
        this.vaultPath = path;
        this.realmName = realmName;
        logger.debugf("PlainTextVaultProvider will operate in %s directory", vaultPath.toAbsolutePath());
    }

    @Override
    public VaultRawSecret obtainSecret(String vaultSecretId) {
        Path secretPath = resolveSecretPath(vaultSecretId);
        if (!Files.exists(secretPath)) {
            logger.warnf("Cannot find secret %s in %s", vaultSecretId, secretPath);
            return DefaultVaultRawSecret.forBuffer(Optional.empty());
        }

        try (FileChannel fileChannel = (FileChannel) Files.newByteChannel(secretPath, EnumSet.of(StandardOpenOption.READ))) {
            MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
            return DefaultVaultRawSecret.forBuffer(Optional.of(mappedByteBuffer));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {

    }

    /**
     * A method that resolves the exact secret location.
     *
     * @param vaultSecretId Secret ID.
     * @return Path for the secret.
     */
    protected Path resolveSecretPath(String vaultSecretId) {
        return vaultPath.resolve(realmName.replaceAll("_", "__") + "_" + vaultSecretId.replaceAll("_", "__"));
    }
}
