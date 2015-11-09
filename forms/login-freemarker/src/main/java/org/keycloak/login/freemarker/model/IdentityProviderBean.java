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

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.services.Urls;

import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class IdentityProviderBean {

    private boolean displaySocial;

    private List<IdentityProvider> providers;
    private RealmModel realm;

    public IdentityProviderBean(RealmModel realm, List<IdentityProviderModel> identityProviders, URI baseURI, UriInfo uriInfo) {
        this.realm = realm;

        if (!identityProviders.isEmpty()) {
            Set<IdentityProvider> orderedSet = new TreeSet<>(IdentityProviderComparator.INSTANCE);
            for (IdentityProviderModel identityProvider : identityProviders) {
                if (identityProvider.isEnabled()) {
                    addIdentityProvider(orderedSet, realm, baseURI, identityProvider);
                }
            }

            if (!orderedSet.isEmpty()) {
                providers = new LinkedList<>(orderedSet);
                displaySocial = true;
            }
        }
    }

    private void addIdentityProvider(Set<IdentityProvider> orderedSet, RealmModel realm, URI baseURI, IdentityProviderModel identityProvider) {
        String loginUrl = Urls.identityProviderAuthnRequest(baseURI, identityProvider.getAlias(), realm.getName()).toString();
        orderedSet.add(new IdentityProvider(identityProvider.getAlias(), identityProvider.getProviderId(), loginUrl,
                identityProvider.getConfig() != null ? identityProvider.getConfig().get("guiOrder") : null));
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
        private final String guiOrder;

        public IdentityProvider(String alias, String providerId, String loginUrl, String guiOrder) {
            this.alias = alias;
            this.providerId = providerId;
            this.loginUrl = loginUrl;
            this.guiOrder = guiOrder;
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

        public String getGuiOrder() {
            return guiOrder;
        }
    }

    public static class IdentityProviderComparator implements Comparator<IdentityProvider> {

        public static IdentityProviderComparator INSTANCE = new IdentityProviderComparator();

        private IdentityProviderComparator() {

        }

        @Override
        public int compare(IdentityProvider o1, IdentityProvider o2) {
            
            int o1order = parseOrder(o1);
            int o2order = parseOrder(o2);

            if (o1order > o2order)
                return 1;
            else if (o1order < o2order)
                return -1;
            
            return 1;
        }

        private int parseOrder(IdentityProvider ip) {
            if (ip != null && ip.getGuiOrder() != null) {
                try {
                    return Integer.parseInt(ip.getGuiOrder());
                } catch (NumberFormatException e) {
                    // ignore it and use defaulr
                }
            }
            return 10000;
        }

    }
}
