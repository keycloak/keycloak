package org.keycloak.vault;

import org.jboss.logging.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * A text-based vault provider, which stores each secret in a separate file. The file name needs to match a vault secret id (or
 * a key for short) and follows the format provided by the configured {@link VaultKeyResolver}. A typical vault directory
 * layout looks like this:
 * <pre>
 *     ${VAULT}/realma__key1 (contains secret for key 1)
 *     ${VAULT}/realma__key2 (contains secret for key 2)
 *     etc...
 * </pre>
 * Note, that in this case each key is prefixed by realm name. This particular kind of layout is used by Kubernetes by default
 * (when mounting a volume into the pod) and can be used by selecting the {@code REALM_UNDERSCORE_KEY} resolver (which is
 * the default resolver when none is defined). Other layouts are available through different resolvers.
 *
 * See https://kubernetes.io/docs/concepts/configuration/secret/
 * See https://github.com/keycloak/keycloak-community/blob/master/design/secure-credentials-store.md#plain-text-file-per-secret-kubernetes--openshift
 *
 * @author Sebastian Łaskawiec
 */
public class FilesPlainTextVaultProvider extends AbstractVaultProvider {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private final Path vaultPath;

    /**
     * Creates a new {@link FilesPlainTextVaultProvider}.
     *
     * @param path A path to a vault. Can not be null.
     * @param realmName A realm name. Can not be null.
     */
    public FilesPlainTextVaultProvider(@Nonnull Path path, @Nonnull String realmName, @Nonnull List<VaultKeyResolver> resolvers) {
        super(realmName, resolvers);
        this.vaultPath = path;
        logger.debugf("PlainTextVaultProvider will operate in %s directory", vaultPath.toAbsolutePath());
    }

    @Override
    protected VaultRawSecret obtainSecretInternal(String vaultSecretId) {
        Path secretPath = vaultPath.resolve(vaultSecretId);
        if (!Files.exists(secretPath)) {
            logger.warnf("Cannot find secret %s in %s", vaultSecretId, secretPath);
            return DefaultVaultRawSecret.forBuffer(Optional.empty());
        }

        try {
            byte[] bytes = Files.readAllBytes(secretPath);
            return DefaultVaultRawSecret.forBuffer(Optional.of(ByteBuffer.wrap(bytes)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {

    }
}
