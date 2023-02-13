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

package org.keycloak.protocol;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * Provides a template/sample client config adapter file.  For example keycloak.json for our OIDC adapter.  keycloak-saml.xml for our SAML client adapter
 *
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ClientInstallationProvider extends Provider, ProviderFactory<ClientInstallationProvider> {
    Response generateInstallation(KeycloakSession session, RealmModel realm, ClientModel client, URI serverBaseUri);
    String getProtocol();
    String getDisplayType();
    String getHelpText();
    String getFilename();
    String getMediaType();
    boolean isDownloadOnly();
}
