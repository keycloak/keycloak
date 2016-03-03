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

package org.keycloak.services;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.*;
import org.keycloak.models.utils.RealmImporter;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.util.LocaleHelper;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Locale;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultKeycloakContext implements KeycloakContext {

    private RealmModel realm;

    private ClientModel client;

    private ClientConnection connection;

    private KeycloakSession session;

    public DefaultKeycloakContext(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public URI getAuthServerUrl() {
        UriInfo uri = getUri();
        KeycloakApplication keycloakApplication = getContextObject(KeycloakApplication.class);
        return keycloakApplication.getBaseUri(uri);
    }

    @Override
    public String getContextPath() {
        KeycloakApplication app = getContextObject(KeycloakApplication.class);
        return app.getContextPath();
    }

    @Override
    public UriInfo getUri() {
        return getContextObject(UriInfo.class);
    }

    @Override
    public HttpHeaders getRequestHeaders() {
        return getContextObject(HttpHeaders.class);
    }

    @Override
    public <T> T getContextObject(Class<T> clazz) {
        return ResteasyProviderFactory.getContextData(clazz);
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public void setRealm(RealmModel realm) {
        this.realm = realm;
    }

    @Override
    public ClientModel getClient() {
        return client;
    }

    @Override
    public void setClient(ClientModel client) {
        this.client = client;
    }

    @Override
    public ClientConnection getConnection() {
        return connection;
    }

    @Override
    public void setConnection(ClientConnection connection) {
        this.connection = connection;
    }

    @Override
    public RealmImporter getRealmManager() {
        RealmManager manager = new RealmManager(session);
        manager.setContextPath(getContextPath());
        return manager;
    }

    @Override
    public Locale resolveLocale(UserModel user) {
        return LocaleHelper.getLocale(session, realm, user);
    }
}
