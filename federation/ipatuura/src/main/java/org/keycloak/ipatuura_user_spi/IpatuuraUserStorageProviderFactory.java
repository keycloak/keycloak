/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.ipatuura_user_spi;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.ipatuura_user_spi.authenticator.IpatuuraAuthenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.resources.admin.ComponentResource;
import org.keycloak.storage.UserStorageProviderFactory;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:jstephen@redhat.com">Justin Stephenson</a>
 * @version $Revision: 1 $
 */
public class IpatuuraUserStorageProviderFactory implements UserStorageProviderFactory<IpatuuraUserStorageProvider>, EnvironmentDependentProviderFactory {

    private static final Logger logger = Logger.getLogger(IpatuuraUserStorageProviderFactory.class);
    public static final String PROVIDER_NAME = "ipatuura";
    protected static final List<String> PROVIDERS = new LinkedList<>();
    protected static final List<ProviderConfigProperty> configMetadata;

    static {
        PROVIDERS.add("ipa");
        PROVIDERS.add("ad");
        PROVIDERS.add("ldap");

        configMetadata = ProviderConfigurationBuilder.create()
                /* SCIMv2 server url */
                .property().name("scimurl").type(ProviderConfigProperty.STRING_TYPE).label("Ipatuura Server URL")
                .helpText("Backend ipatuura server URL in the format: server.example.com:8080")
                .add()
                /* Login username, used to auth to make HTTP requests */
                .property().name("loginusername").type(ProviderConfigProperty.STRING_TYPE).label("Login username")
                .helpText("Username to authenticate through the server")
                .add()
                /* Login password, used to auth to make HTTP requests */
                .property().name("loginpassword").type(ProviderConfigProperty.PASSWORD).label("Login password")
                .helpText("password to authenticate through the server")
                .secret(true).add().build();
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configMetadata;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config)
            throws ComponentValidationException {
        if (config.getId() != null) {
            // Updating an existing component - make sure the stored login password isn't silently sent
            // to a different server/user if the URL or username changed but the password wasn't re-entered.
            validatePasswordReentryOnServerOrUserChange(session, realm, config);
        }

        Ipatuura ipatuura = new Ipatuura(session, config);

        SimpleHttpResponse response;

        try {
            response = ipatuura.clientRequest("", "GET", null);
            response.close();
        } catch (Exception e) {
            throw new ComponentValidationException("Cannot connect to provided URL!", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void validatePasswordReentryOnServerOrUserChange(KeycloakSession session, RealmModel realm, ComponentModel config) {
        ComponentModel oldComponent = realm.getComponent(config.getId());
        if (oldComponent == null) {
            return;
        }

        Set<String> secretPlaceholderFields = session.getAttribute(
                ComponentResource.SECRET_PLACEHOLDER_FIELDS_ATTR, Set.class);
        if (secretPlaceholderFields == null || !secretPlaceholderFields.contains("loginpassword")) {
            // The login password was explicitly re-entered, no risk of sending it to a different destination.
            return;
        }

        String oldUrl = oldComponent.getConfig().getFirst("scimurl");
        String newUrl = config.getConfig().getFirst("scimurl");
        String oldUsername = oldComponent.getConfig().getFirst("loginusername");
        String newUsername = config.getConfig().getFirst("loginusername");

        if (requiresPasswordReentry(oldUrl, newUrl, oldUsername, newUsername)) {
            throw new ComponentValidationException(
                    "The login password must be re-entered when the Ipatuura Server URL or login username is changed");
        }
    }

    /**
     * Determines whether the login password must be re-entered because the server URL or the
     * username it is bound to has changed, instead of silently reusing the previously stored password.
     */
    static boolean requiresPasswordReentry(String oldUrl, String newUrl, String oldUsername, String newUsername) {
        return !Objects.equals(oldUrl, newUrl) || !Objects.equals(oldUsername, newUsername);
    }

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }

    @Override
    public IpatuuraUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        Ipatuura ipatuura = new Ipatuura(session, model);
        return new IpatuuraUserStorageProvider(session, model, ipatuura, this);
    }

    protected IpatuuraAuthenticator createSCIMAuthenticator() {
        return new IpatuuraAuthenticator();
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.IPA_TUURA_FEDERATION);
    }
}
