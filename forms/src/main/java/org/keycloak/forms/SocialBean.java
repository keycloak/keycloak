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
package org.keycloak.forms;

import org.keycloak.forms.model.SocialProvider;
import org.keycloak.services.resources.flows.Urls;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SocialBean {

    private RealmBean realm;

    private RegisterBean registerBean;

    private UrlBean url;

    private List<SocialProvider> providers;

    public SocialBean(RealmBean realm, List<org.keycloak.social.SocialProvider> providers, RegisterBean registerBean, UrlBean url) {
        this.realm = realm;
        this.registerBean = registerBean;
        this.url = url;

        URI baseURI = url.getBaseURI();

        UriBuilder socialLoginUrlBuilder = UriBuilder.fromUri(Urls.socialRedirectToProviderAuth(baseURI, realm.getId()));

        this.providers = new LinkedList<SocialProvider>();
        for (org.keycloak.social.SocialProvider p : providers) {
            String loginUrl = socialLoginUrlBuilder.replaceQueryParam("provider_id", p.getId()).build().toString();
            this.providers.add(new SocialProvider(p.getId(), p.getName(), loginUrl));
        }
    }

    public List<SocialProvider> getProviders() {
        return providers;
    }

    // Display panel with social providers just in case that social is enabled for realm, but we are not in the middle of registration with social
    public boolean isDisplaySocialProviders() {
        return realm.isSocial() && !providers.isEmpty();
    }

    public RealmBean getRealm() {
        return realm;
    }

    public void setRealm(RealmBean realm) {
        this.realm = realm;
    }

    public UrlBean getUrl() {
        return url;
    }

    public void setUrl(UrlBean url) {
        this.url = url;
    }

    public RegisterBean getRegisterBean() {
        return registerBean;
    }

    public void setRegisterBean(RegisterBean registerBean) {
        this.registerBean = registerBean;
    }
}
