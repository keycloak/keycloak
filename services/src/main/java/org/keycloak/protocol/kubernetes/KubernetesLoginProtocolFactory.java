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
package org.keycloak.protocol.kubernetes;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.util.UriUtils;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientTemplateRepresentation;
import org.keycloak.services.ServicesLogger;

import java.util.*;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KubernetesLoginProtocolFactory extends OIDCLoginProtocolFactory {
    private static final Logger logger = Logger.getLogger(KubernetesLoginProtocolFactory.class);
    public static final String LOGIN_PROTOCOL = "kubernetes";

    @Override
    public LoginProtocol create(KeycloakSession session) {
        return new Kubernetes().setSession(session);
    }

    @Override
    protected void addDefaults(ClientModel client) {
        // OIDC will add defaults
    }

    @Override
    public Object createProtocolEndpoint(RealmModel realm, EventBuilder event) {
        return new KubernetesProtocolEndpoint(realm, event);
    }

    @Override
    public String getId() {
        return LOGIN_PROTOCOL;
    }

    @Override
    public void setupClientDefaults(ClientRepresentation rep, ClientModel newClient) {
        if (rep.getRootUrl() != null && (rep.getRedirectUris() == null || rep.getRedirectUris().isEmpty())) {
            String root = rep.getRootUrl();
            if (root.endsWith("/")) root = root + "*";
            else root = root + "/*";
            newClient.addRedirectUri(root);

            Set<String> origins = new HashSet<String>();
            String origin = UriUtils.getOrigin(root);
            logger.debugv("adding default client origin: {0}" , origin);
            origins.add(origin);
            newClient.setWebOrigins(origins);
        }
        if (rep.isBearerOnly() == null
                && rep.isPublicClient() == null) {
            newClient.setPublicClient(true);
        }
        if (rep.isBearerOnly() == null) newClient.setBearerOnly(false);

        // Backwards compatibility only
        if (rep.isDirectGrantsOnly() != null) {
            ServicesLogger.LOGGER.usingDeprecatedDirectGrantsOnly();
            newClient.setStandardFlowEnabled(!rep.isDirectGrantsOnly());
            newClient.setDirectAccessGrantsEnabled(rep.isDirectGrantsOnly());
        } else {
            if (rep.isStandardFlowEnabled() == null) newClient.setStandardFlowEnabled(true);
            if (rep.isDirectAccessGrantsEnabled() == null) newClient.setDirectAccessGrantsEnabled(true);

        }

        if (rep.isImplicitFlowEnabled() == null) newClient.setImplicitFlowEnabled(true);
        if (rep.isPublicClient() == null) newClient.setPublicClient(true);
        if (rep.isFrontchannelLogout() == null) newClient.setFrontchannelLogout(false);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public Map<String, ProtocolMapperModel> getBuiltinMappers() {
        return Collections.EMPTY_MAP;
    }

    @Override
    protected void createDefaultClientScopesImpl(RealmModel newRealm) {
    }
}
