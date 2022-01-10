/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.util.FindFile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.util.JsonSerialization;
import org.keycloak.wellknown.WellKnownProvider;
import org.keycloak.wellknown.WellKnownProviderFactory;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OIDCWellKnownProviderFactory implements WellKnownProviderFactory {

    public static final String PROVIDER_ID = "openid-configuration";

    private static final Logger logger = Logger.getLogger(OIDCWellKnownProviderFactory.class);

    private Map<String, Object> openidConfigOverride = null;
    private boolean includeClientScopes = true;

    @Override
    public WellKnownProvider create(KeycloakSession session) {
        return new OIDCWellKnownProvider(session, openidConfigOverride, includeClientScopes);
    }

    @Override
    public void init(Config.Scope config) {
        String openidConfigurationOverride = config.get("openid-configuration-override");
        this.includeClientScopes = config.getBoolean("include-client-scopes", true);
        logger.debugf("Include Client Scopes in OIDC Well-known endpoint: %s", this.includeClientScopes);
        if (openidConfigurationOverride != null) {
            initConfigOverrideFromFile(openidConfigurationOverride);
        }
    }

    protected void initConfigOverrideFromFile(String openidConfigurationOverrideFile) {
        try {
            InputStream is = FindFile.findFile(openidConfigurationOverrideFile);
            this.openidConfigOverride = JsonSerialization.readValue(is, Map.class);
            logger.infof("Overriding default OIDC well-known endpoint configuration with the options from file '%s'", openidConfigurationOverrideFile);
        } catch (RuntimeException re) {
            logger.warnf(re, "Unable to find file specified for openid-configuration-override on custom location '%s'. Will stick to the default configuration for OIDC WellKnown endpoint", openidConfigurationOverrideFile);
        } catch (IOException ioe) {
            logger.warnf(ioe, "Error when trying to deserialize JSON from the file '%s'. Check the JSON format. Will stick to the default configuration for OIDC WellKnown endpoint", openidConfigurationOverrideFile);
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    // Custom implementation with alias "openid-configuration" should win over this default one
    @Override
    public int getPriority() {
        return 100;
    }

    protected Map<String, Object> getOpenidConfigOverride() {
        return openidConfigOverride;
    }
}
