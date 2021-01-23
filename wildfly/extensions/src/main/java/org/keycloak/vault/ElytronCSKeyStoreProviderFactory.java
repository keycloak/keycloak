/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.vault;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.wildfly.security.auth.SupportLevel;
import org.wildfly.security.auth.server.IdentityCredentials;
import org.wildfly.security.credential.Credential;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.source.CredentialSource;
import org.wildfly.security.credential.store.CredentialStore;
import org.wildfly.security.credential.store.CredentialStoreException;
import org.wildfly.security.credential.store.WildFlyElytronCredentialStoreProvider;
import org.wildfly.security.credential.store.impl.KeyStoreCredentialStore;
import org.wildfly.security.password.interfaces.ClearPassword;
import org.wildfly.security.util.PasswordBasedEncryptionUtil;

/**
 * A {@link VaultProviderFactory} implementation that creates and configures {@link ElytronCSKeyStoreProvider}s. The following
 * configuration attributes are available for the {@code ElytronCSKeyStoreProviderFactory}:
 * <ul>
 *     <li><b>location (required)</b>: the path to he keystore file that contains the secrets. This file is created and managed by Elytron
 *     using either the {@code elytron} subsystem in WildFly/EAP or the {@code elytron-tool.sh} script.</li>
 *     <li><b>secret (required)</b>: the keystore master secret. Can be specified in clear text form or in masked form. The masked form
 *     can be generated using the {@code elytron-tool.sh} script. For further details, check the Elytron tool documentation.</li>
 *     <li><b>keyStoreType (optional)</b>: the keystore type. Defaults to {@code JCEKS}.</li>
 *     <li><b>keyResolvers (optional)</b>: a comma-separated list of vault key resolvers. Defaults to {@code REALM_UNDERSCORE_KEY}.</li>
 * </ul>
 * <p/>
 * If any of the required configuration attributes is missing, the factory logs a debug message indicating that it has not
 * been properly configured and will return {@code null} when {@link #create(KeycloakSession)} is called.
 * <p/>
 * If the factory has been properly configured but the {@code location} attribute points to a keystore that does not exist,
 * a {@link VaultNotFoundException} is raised on init. Similarly, if the key resolvers are configured and none of the specified
 * resolvers is valid, a {@link VaultConfigurationException} is raised on init.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class ElytronCSKeyStoreProviderFactory extends AbstractVaultProviderFactory {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private static final String PROVIDER_ID = "elytron-cs-keystore";

    static final String CS_LOCATION = "location";
    static final String CS_SECRET = "secret";
    static final String CS_KEYSTORE_TYPE = "keyStoreType";
    static final String JCEKS = "JCEKS";

    private String credentialStoreLocation;
    private String credentialStoreType;
    private String credentialStoreSecret;

    @Override
    public VaultProvider create(KeycloakSession session) {
        if (this.credentialStoreLocation == null || this.credentialStoreSecret == null) {
            logger.debug("Can not create an elytron-based vault provider since it's not initialized correctly");
            return null;
        }
        Map<String, String> attributes = new HashMap<>();
        attributes.put(CS_LOCATION, this.credentialStoreLocation);
        attributes.put(CS_KEYSTORE_TYPE, this.credentialStoreType);

        CredentialStore credentialStore;
        try {
            credentialStore = CredentialStore.getInstance(KeyStoreCredentialStore.KEY_STORE_CREDENTIAL_STORE);
            credentialStore.initialize(attributes, new CredentialStore.CredentialSourceProtectionParameter(
                    this.getCredentialSource(this.credentialStoreSecret)));
        } catch (NoSuchAlgorithmException | CredentialStoreException e) {
            logger.debug("Error instantiating credential store", e);
            return null;
        }

        return new ElytronCSKeyStoreProvider(credentialStore, getRealmName(session), super.keyResolvers);
    }

    @Override
    public void init(Config.Scope config) {
        super.init(config);

        this.credentialStoreLocation = config.get(CS_LOCATION);
        if (this.credentialStoreLocation == null) {
            logger.debug("ElytronCSKeyStoreProviderFactory not properly configured - missing store location");
            return;
        }
        if (!Files.exists(Paths.get(this.credentialStoreLocation))) {
            throw new VaultNotFoundException("The " + this.credentialStoreLocation + " file doesn't exist");
        }

        this.credentialStoreSecret = config.get(CS_SECRET);
        if (this.credentialStoreSecret == null) {
            logger.debug("ElytronCSKeyStoreProviderFactory not properly configured - missing store secret");
            return;
        }
        this.credentialStoreType = config.get(CS_KEYSTORE_TYPE, JCEKS);

        // install the elytron credential store provider.
        Security.addProvider(WildFlyElytronCredentialStoreProvider.getInstance());
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
        // remove the elytron credential store provider.
        Security.removeProvider(WildFlyElytronCredentialStoreProvider.getInstance().getName());
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    /**
     * Obtains the {@code CredentialSource} to be used as a protection parameter when initializing the Elytron credential
     * store. The source is essentially a wrapper for the credential store secret. The credential store secret can be specified
     * in clear text form or in masked form. Check the Elytron tool documentation for instruction on how to mask the credential
     * store secret.
     * <p/>
     * <b>Note: </b>This logic should ideally be provided directly by Elytron but is currently missing.
     *
     * @param secret the secret obtained from the {@link ElytronCSKeyStoreProviderFactory} configuration.
     * @return the constructed {@code CredentialSource}.
     */
    protected CredentialSource getCredentialSource(final String secret) {
        if (secret != null && secret.startsWith("MASK-")) {
            return new CredentialSource() {
                @Override
                public SupportLevel getCredentialAcquireSupport(Class<? extends Credential> credentialType, String algorithmName, AlgorithmParameterSpec parameterSpec) throws IOException {
                    return credentialType == PasswordCredential.class ? SupportLevel.SUPPORTED : SupportLevel.UNSUPPORTED;
                }

                @Override
                public <C extends Credential> C getCredential(Class<C> credentialType, String algorithmName, AlgorithmParameterSpec parameterSpec) throws IOException {
                    String[] part = secret.substring(5).split(";");  // strip "MASK-" and split by ';'
                    if (part.length != 3) {
                        throw new IOException("Masked password command has the wrong format.%nUsage: MASK-<encoded secret>;<salt>;<iteration count> " +
                                "where <salt>=UTF-8 characters, <iteration count>=reasonable sized positive integer");
                    }
                    String salt = part[1];
                    final int iterationCount;
                    try {
                        iterationCount = Integer.parseInt(part[2]);
                    } catch (NumberFormatException e) {
                        throw new IOException("Masked password command has the wrong format.%nUsage: MASK-<encoded secret>;<salt>;<iteration count> " +
                                "where <salt>=UTF-8 characters, <iteration count>=reasonable sized positive integer");
                    }
                    try {
                        PasswordBasedEncryptionUtil decryptUtil = new PasswordBasedEncryptionUtil.Builder()
                                .picketBoxCompatibility().salt(salt).iteration(iterationCount).decryptMode()
                                .build();
                        return credentialType.cast(new PasswordCredential(ClearPassword.createRaw(ClearPassword.ALGORITHM_CLEAR,
                                decryptUtil.decodeAndDecrypt(part[0]))));
                    } catch (GeneralSecurityException e) {
                        throw new IOException(e);
                    }
                }
            };
        } else {
            return IdentityCredentials.NONE.withCredential(new PasswordCredential(
                    ClearPassword.createRaw(ClearPassword.ALGORITHM_CLEAR, secret.toCharArray())));
        }
    }


}
