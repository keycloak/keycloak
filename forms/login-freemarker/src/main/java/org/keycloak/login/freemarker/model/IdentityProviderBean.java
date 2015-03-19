/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.login.freemarker.model;

import org.keycloak.OAuth2Constants;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.flows.Urls;

import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class IdentityProviderBean {

    private boolean displaySocial;

    private List<IdentityProvider> providers;
    private RealmModel realm;

    public IdentityProviderBean(RealmModel realm, URI baseURI, UriInfo uriInfo) {
        this.realm = realm;
        List<IdentityProviderModel> identityProviders = realm.getIdentityProviders();

        if (!identityProviders.isEmpty()) {
            providers = new LinkedList<IdentityProvider>();

            for (IdentityProviderModel identityProvider : identityProviders) {
                if (identityProvider.isEnabled()) {
                    addIdentityProvider(realm, baseURI, identityProvider);
                }
            }

            if (!providers.isEmpty()) {
                displaySocial = true;
            }
        }
    }

    private void addIdentityProvider(RealmModel realm, URI baseURI, IdentityProviderModel identityProvider) {
        String loginUrl = Urls.identityProviderAuthnRequest(baseURI, identityProvider.getAlias(), realm.getName()).toString();
        providers.add(new IdentityProvider(identityProvider.getAlias(), identityProvider.getProviderId(), loginUrl));
    }

    public List<IdentityProvider> getProviders() {
        return providers;
    }

    public boolean isDisplayInfo() {
        return  realm.isRegistrationAllowed() || displaySocial;
    }

    public static class IdentityProvider {

        private final String alias;
        private final String providerId; // This refer to providerType (facebook, google, etc.)
        private final String loginUrl;

        public IdentityProvider(String alias, String providerId,String loginUrl) {
            this.alias = alias;
            this.providerId = providerId;

            this.loginUrl = loginUrl;
        }

        public String getAlias() {
            return alias;
        }

        public String getLoginUrl() {
            return loginUrl;
        }

        public String getProviderId() {
            return providerId;
        }
    }
}
