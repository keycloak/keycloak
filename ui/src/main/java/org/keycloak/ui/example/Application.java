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
package org.keycloak.ui.example;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@XmlRootElement
public class Application {

    private String[] callbackUrl;

    private boolean enabled;

    private String id;

    private String[] initialRoles;

    private String name;

    private String realm;

    private String[] roles;

    public String[] getCallbackUrl() {
        return callbackUrl;
    }

    public String getId() {
        return id;
    }

    public String[] getInitialRoles() {
        return initialRoles;
    }

    public String getName() {
        return name;
    }

    public String getRealm() {
        return realm;
    }

    public String[] getRoles() {
        return roles;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setCallbackUrl(String[] callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setInitialRoles(String[] initialRoles) {
        this.initialRoles = initialRoles;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public void setRoles(String[] roles) {
        this.roles = roles;
    }

}
