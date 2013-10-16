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

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

/**
 * @author <a href="mailto:vrockai@redhat.com">Viliam Rockai</a>
 */
public class OAuthGrantBean {

    private MultivaluedMap<String, RoleModel> resourceRolesRequested;
    private List<RoleModel> realmRolesRequested;
    private UserModel client;
    private String oAuthCode;
    private String action;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public List<String> getResourceNames(){
         return new ArrayList<String>(resourceRolesRequested.keySet());
    }

    public MultivaluedMap<String, RoleModel> getResourceRolesRequested() {
        return resourceRolesRequested;
    }

    public void setResourceRolesRequested(MultivaluedMap<String, RoleModel> resourceRolesRequested) {
        this.resourceRolesRequested = resourceRolesRequested;
    }

    public List<RoleModel> getRealmRolesRequested() {
        return realmRolesRequested;
    }

    public void setRealmRolesRequested(List<RoleModel> realmRolesRequested) {
        this.realmRolesRequested = realmRolesRequested;
    }

    public UserModel getClient() {
        return client;
    }

    public void setClient(UserModel client) {
        this.client = client;
    }

    // lowercase "o" needed for FM template to access the property
    public String getoAuthCode() {
        return oAuthCode;
    }

    public void setoAuthCode(String oAuthCode) {
        this.oAuthCode = oAuthCode;
    }

}
