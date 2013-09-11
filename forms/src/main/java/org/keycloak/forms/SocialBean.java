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

import java.net.URI;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.imageio.spi.ServiceRegistry;
import javax.ws.rs.core.UriBuilder;

import org.keycloak.forms.model.SocialProvider;
import org.keycloak.services.resources.flows.Urls;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@ManagedBean(name = "social")
@RequestScoped
public class SocialBean {

    @ManagedProperty(value = "#{realm}")
    private RealmBean realm;

    @ManagedProperty(value = "#{register}")
    private RegisterBean registerBean;

    @ManagedProperty(value = "#{url}")
    private UrlBean url;

    private List<SocialProvider> providers;

    private UriBuilder socialLoginUrlBuilder;

    @PostConstruct
    public void init() {
        URI baseURI = url.getBaseURI();

        socialLoginUrlBuilder = UriBuilder.fromUri(Urls.socialRedirectToProviderAuth(baseURI, realm.getId()));

        providers = new LinkedList<SocialProvider>();
        for (Iterator<org.keycloak.social.SocialProvider> itr = ServiceRegistry
                .lookupProviders(org.keycloak.social.SocialProvider.class); itr.hasNext();) {
            org.keycloak.social.SocialProvider p = itr.next();

            String loginUrl = socialLoginUrlBuilder.replaceQueryParam("provider_id", p.getId()).build().toString();
            providers.add(new SocialProvider(p.getId(), p.getName(), loginUrl));
        }
    }

    public List<SocialProvider> getProviders() {
        return providers;
    }

    // Display panel with social providers just in case that social is enabled for realm, but we are not in the middle of registration with social
    public boolean isDisplaySocialProviders() {
        return realm.isSocial() && !registerBean.isSocialRegistration();
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
