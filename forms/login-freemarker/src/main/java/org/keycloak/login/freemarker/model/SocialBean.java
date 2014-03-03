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

import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.flows.Urls;
import org.keycloak.social.SocialLoader;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SocialBean {

    private boolean displaySocial;

    private List<SocialProvider> providers;
    private RealmModel realm;

    public SocialBean(RealmModel realm, URI baseURI) {
        this.realm = realm;
        Map<String, String> socialConfig = realm.getSocialConfig();
        if (realm.isSocial() && !socialConfig.isEmpty()) {
            displaySocial = true;
            providers = new LinkedList<SocialProvider>();

            UriBuilder socialLoginUrlBuilder = UriBuilder.fromUri(Urls.socialRedirectToProviderAuth(baseURI, realm.getName()));
            for (org.keycloak.social.SocialProvider p : SocialLoader.load()) {
                if (socialConfig.containsKey(p.getId() + ".key") && socialConfig.containsKey(p.getId() + ".secret")) {
                    String loginUrl = socialLoginUrlBuilder.replaceQueryParam("provider_id", p.getId()).build().toString();
                    providers.add(new SocialProvider(p.getId(), p.getName(), loginUrl));
                }
            }
        }
    }

    public List<SocialProvider> getProviders() {
        return providers;
    }

    public boolean isDisplayInfo() {
        return  realm.isRegistrationAllowed() || displaySocial;
    }

    public boolean isDisplaySocialProviders() {
        return displaySocial;
    }

    public static class SocialProvider {

        private String id;
        private String name;
        private String loginUrl;

        public SocialProvider(String id, String name, String loginUrl) {
            this.id = id;
            this.name = name;
            this.loginUrl = loginUrl;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getLoginUrl() {
            return loginUrl;
        }

    }

}
