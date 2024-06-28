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

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.LinkedList;
import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;

/**
 * Abstract class that is meant to be extended by implementations of {@link VaultProviderFactory} that want to offer support
 * for the configuration of key resolvers.
 * <p/>
 * It implements the {@link #init(Config.Scope)} method, where is looks for the {@code keyResolvers} property. The value is
 * a comma-separated list of key resolver names. It then verifies if the resolver names match one of the available key resolver
 * implementations and then creates a list of {@link VaultKeyResolver} instances that subclasses can pass to {@link VaultProvider}
 * instances on {@link #create(KeycloakSession)}.
 * <p/>
 * The list of currently available resolvers follows:
 * <ul>
 *     <li>{@code KEY_ONLY}: only the key name is used as is, realm is ignored;</li>
 *     <li>{@code REALM_UNDERSCORE_KEY}: realm and key are combined using an underscore ({@code '_'}) character. Any occurrences of
 *     underscore in both the realm and key are escaped by an additional underscore character;</li>
 *     <li>{@code REALM_FILESEPARATOR_KEY}: realm and key are combined using the platform file separator character. It might not be
 *     suitable for every vault provider but it enables the grouping of secrets using a directory structure;</li>
 *     <li>{@code FACTORY_PROVIDED}: the format of the constructed key is determined by the factory's {@link #getFactoryResolver()}
 *     implementation. it allows for the customization of the final key format by extending the factory and overriding the
 *     {@link #getFactoryResolver()} method.</li>
 * </ul>
 * <p/>
 * <b><i>Note</i></b>: When extending the standard factories to use the {@code FACTORY_PROVIDED} resolver, it is important to also
 * override the {@link #getId()} method so that the custom factory has its own id and as such can be configured in the keycloak
 * server.
 * <p/>
 * If no resolver is explicitly configured for the factory, it defaults to using the {@code REALM_UNDERSCORE_KEY} resolver.
 * When one or more resolvers are explicitly configured, this factory iterates through them in order and for each one attempts
 * to obtain the respective {@link VaultKeyResolver} implementation. If it fails (for example, the name doesn't match one of
 * the existing resolvers), it logs a message and ignores the resolver. If it fails to load all configured resolvers, it
 * throws a {@link VaultConfigurationException}.
 * <p/>
 * Concrete implementations must also make sure to call the {@code super.init(config)} in their own {@link #init(Config.Scope)}
 * implementations so tha the processing of the key resolvers is performed correctly.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public abstract class AbstractVaultProviderFactory implements VaultProviderFactory {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    protected static final String KEY_RESOLVERS = "keyResolvers";

    protected List<VaultKeyResolver> keyResolvers = new LinkedList<>();

    @Override
    public void init(Config.Scope config) {
        String resolverNames = config.get(KEY_RESOLVERS);
        if (resolverNames != null) {
            for (String resolverName : resolverNames.split(",")) {
                VaultKeyResolver resolver = this.getVaultKeyResolver(resolverName);
                if (resolver != null) {
                    this.keyResolvers.add(resolver);
                }
            }
            if (this.keyResolvers.isEmpty()) {
                throw new VaultConfigurationException("Unable to initialize factory - all provided key resolvers are invalid");
            }
        }
        // no resolver configured - add the default REALM_UNDERSCORE_KEY resolver.
        if (this.keyResolvers.isEmpty()) {
            logger.debugf("Key resolver is undefined - using %s by default", AvailableResolvers.REALM_UNDERSCORE_KEY.name());
            this.keyResolvers.add(AvailableResolvers.REALM_UNDERSCORE_KEY.getVaultKeyResolver());
        }
    }

    /**
     * Obtains the {@link VaultKeyResolver} implementation that is provided by the factory itself. By default this method
     * throws an {@link UnsupportedOperationException}, so an attempt to use the {@code FACTORY_PROVIDED} resolver on a
     * factory that doesn't override this method will result in a failure to use this resolver.
     *
     * @return the factory-provided {@link VaultKeyResolver}.
     */
    protected VaultKeyResolver getFactoryResolver() {
        throw new UnsupportedOperationException("getFactoryResolver not implemented by factory " + getClass().getName());
    }

    /**
     * Obtains the name of realm from the {@link KeycloakSession}.
     *
     * @param session a reference to the {@link KeycloakSession}.
     * @return the name of the realm.
     */
    protected String getRealmName(KeycloakSession session) {
        return session.getContext().getRealm().getName();
    }

    /**
     * Obtains the key resolver with the specified name.
     *
     * @param resolverName the name of the resolver.
     * @return the {@link VaultKeyResolver} that corresponds to the name or {@code null} if the resolver could not be retrieved.
     */
    private VaultKeyResolver getVaultKeyResolver(final String resolverName) {
        try {
            AvailableResolvers value = AvailableResolvers.valueOf(resolverName.trim().toUpperCase());
            return value == AvailableResolvers.FACTORY_PROVIDED ? this.getFactoryResolver() : value.getVaultKeyResolver();
        }
        catch(Exception e) {
            logger.debugf(e,"Invalid key resolver: %s - skipping", resolverName);
            return null;
        }
    }

    /**
     * Enum containing the available {@link VaultKeyResolver}s. The name used in the factory configuration must match the
     * name one of the enum members.
     */
    protected enum AvailableResolvers {

        /**
         * Ignores the realm, only the vault key is used when retrieving a secret from the vault. This is useful when we want
         * all realms to share the secrets, so instead of replicating entries for all existing realms in the vault one can
         * simply use key directly and all realms will obtain the same secret.
         */
        KEY_ONLY((realm, key) -> key),

        /**
         * The realm is prepended to the vault key and they are separated by an underscore ({@code '_'}) character. If either
         * the realm or the key contains an underscore, it is escaped by another underscore character.
         */
        REALM_UNDERSCORE_KEY((realm, key) -> realm.replaceAll("_", "__") + "_" + key.replaceAll("_", "__")),

        /**
         * The realm is prepended to the vault key and they are separated by the platform file separator character. Not all
         * providers might support this format but it is useful when a directory structure is used to group secrets per realm.
         */
        REALM_FILESEPARATOR_KEY((realm, key) -> realm + File.separator + key),

        /**
         * The format of the vault key is determined by the factory's {@code getFactoryResolver} implementation. This allows
         * for the customization of the vault key format by extending the factory and overriding the {@code getFactoryResolver}
         * method. It is instantiated with a null resolver because we can't access the factory from the enum's static context.
         */
        FACTORY_PROVIDED(null);

        private VaultKeyResolver resolver;

        AvailableResolvers(final VaultKeyResolver resolver) {
            this.resolver = resolver;
        }

        VaultKeyResolver getVaultKeyResolver() {
            return this.resolver;
        }
    }
}
