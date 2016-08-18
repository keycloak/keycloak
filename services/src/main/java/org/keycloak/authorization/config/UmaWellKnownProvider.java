/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.authorization.config;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.wellknown.WellKnownProvider;

import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UmaWellKnownProvider implements WellKnownProvider {

    private final KeycloakSession session;

    public UmaWellKnownProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getConfig() {
        RealmModel realm = this.session.getContext().getRealm();
        UriInfo uriInfo = this.session.getContext().getUri();

        return Configuration.fromDefault(RealmsResource.realmBaseUrl(uriInfo).build(realm.getName()).toString(), realm.getName(),
                URI.create(RealmsResource.protocolUrl(uriInfo).path(OIDCLoginProtocolService.class, "auth").build(realm.getName(), OIDCLoginProtocol.LOGIN_PROTOCOL).toString()),
                URI.create(RealmsResource.protocolUrl(uriInfo).path(OIDCLoginProtocolService.class, "token").build(realm.getName(), OIDCLoginProtocol.LOGIN_PROTOCOL).toString()),
                realm.getPublicKeyPem());
    }

    @Override
    public void close() {

    }
}
