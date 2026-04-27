/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.protocol.oidc.encode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.oidc.grants.OAuth2GrantType;
import org.keycloak.protocol.oidc.grants.OAuth2GrantTypeFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultTokenContextEncoderProviderFactory implements TokenContextEncoderProviderFactory {

    private KeycloakSessionFactory sessionFactory;
    private Map<String, AccessTokenContext.SessionType> sessionTypesByShortcut;
    private Map<String, AccessTokenContext.TokenType> tokenTypesByShortcut;
    Map<String, String> grantsByShortcuts;
    Map<String, String> grantsToShortcuts;

    @Override
    public TokenContextEncoderProvider create(KeycloakSession session) {
        return new DefaultTokenContextEncoderProvider(session, this);
    }

    @Override
    public void init(Config.Scope config) {
        sessionTypesByShortcut = new HashMap<>();
        for (AccessTokenContext.SessionType st : AccessTokenContext.SessionType.values()) {
            sessionTypesByShortcut.put(st.getShortcut(), st);
        }
        sessionTypesByShortcut = Collections.unmodifiableMap(sessionTypesByShortcut);

        tokenTypesByShortcut = new HashMap<>();
        for (AccessTokenContext.TokenType tt : AccessTokenContext.TokenType.values()) {
            tokenTypesByShortcut.put(tt.getShortcut(), tt);
        }
        tokenTypesByShortcut = Collections.unmodifiableMap(tokenTypesByShortcut);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        this.sessionFactory = factory;
        grantsByShortcuts = new ConcurrentHashMap<>();
        grantsToShortcuts = new ConcurrentHashMap<>();

        factory.getProviderFactoriesStream(OAuth2GrantType.class)
                .forEach((factory1) -> {
                    OAuth2GrantTypeFactory gtf = (OAuth2GrantTypeFactory) factory1;
                    String grantName = gtf.getId();
                    String grantShortcut = gtf.getShortcut();
                    grantsByShortcuts.put(grantShortcut, grantName);
                    grantsToShortcuts.put(grantName, grantShortcut);
                });
        grantsByShortcuts.put(DefaultTokenContextEncoderProvider.UNKNOWN, DefaultTokenContextEncoderProvider.UNKNOWN);
        grantsToShortcuts.put(DefaultTokenContextEncoderProvider.UNKNOWN, DefaultTokenContextEncoderProvider.UNKNOWN);

        // Validation if there are not duplicated shortcuts (for example when introducing new grant impl...)
        if (grantsByShortcuts.size() != grantsToShortcuts.size()) {
            throw new IllegalStateException("Different lengths of maps. grantsByShortcuts.size=" + grantsByShortcuts.size() + ", grantsToShortcuts.size=" + grantsToShortcuts.size() +
                    ". Make sure that there is no OAuth2GrantType implementation with same ID or shortcut like other grants");
        }
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "default";
    }

    protected AccessTokenContext.SessionType getSessionTypeByShortcut(String sessionTypeShortcut) {
        return sessionTypesByShortcut.get(sessionTypeShortcut);
    }

    protected AccessTokenContext.TokenType getTokenTypeByShortcut(String tokenTypeShortcut) {
        return tokenTypesByShortcut.get(tokenTypeShortcut);
    }

    protected String getShortcutByGrantType(String grantType) {
        String grantShortcut = grantsToShortcuts.get(grantType);
        if (grantShortcut == null) {
            // Refresh maps in case new grant type was deployed
            OAuth2GrantTypeFactory factory = (OAuth2GrantTypeFactory) sessionFactory.getProviderFactory(OAuth2GrantType.class, grantType);
            if (factory != null) {
                String shortcut = factory.getShortcut();
                grantsByShortcuts.put(shortcut, grantType);
                grantsToShortcuts.put(grantType, shortcut);
            }
            grantShortcut = grantsToShortcuts.get(grantType);
        }
        return grantShortcut;
    }

    protected String getGrantTypeByShortcut(String shortcut) {
        String grantType = grantsByShortcuts.get(shortcut);
        if (grantType == null) {
            // Refresh maps in case new grant type was deployed
            sessionFactory.getProviderFactoriesStream(OAuth2GrantType.class)
                    .map(fct -> (OAuth2GrantTypeFactory) fct)
                    .filter(fct -> shortcut.equals(fct.getShortcut()))
                    .map(OAuth2GrantTypeFactory::getId)
                    .findFirst()
                    .ifPresent(newGrantType -> {
                        grantsByShortcuts.put(shortcut, newGrantType);
                        grantsToShortcuts.put(newGrantType, shortcut);
                    });
            grantType = grantsByShortcuts.get(shortcut);
        }
        return grantType;
    }
}
