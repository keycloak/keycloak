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

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Realm {

    private boolean enabled;

    private String[] initialRoles;

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private String name;

    private String[] roles;

    private boolean social;

    private long tokenExpiration;

    private TimeUnit tokenExpirationUnit;

    private boolean userRegistration;

    public String[] getInitialRoles() {
        return initialRoles;
    }

    public String getName() {
        return name;
    }

    public String[] getRoles() {
        return roles;
    }

    public long getTokenExpiration() {
        return tokenExpiration;
    }

    public TimeUnit getTokenExpirationUnit() {
        return tokenExpirationUnit;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isSocial() {
        return social;
    }

    public boolean isUserRegistration() {
        return userRegistration;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setInitialRoles(String[] initialRoles) {
        this.initialRoles = initialRoles;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRoles(String[] roles) {
        this.roles = roles;
    }

    public void setSocial(boolean social) {
        this.social = social;
    }

    public void setTokenExpiration(long tokenExpiration) {
        this.tokenExpiration = tokenExpiration;
    }

    public void setTokenExpirationUnit(TimeUnit tokenExpirationUnit) {
        this.tokenExpirationUnit = tokenExpirationUnit;
    }

    public void setUserRegistration(boolean userRegistration) {
        this.userRegistration = userRegistration;
    }

}
