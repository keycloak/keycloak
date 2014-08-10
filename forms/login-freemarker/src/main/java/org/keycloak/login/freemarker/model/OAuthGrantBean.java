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

import org.keycloak.models.ClaimMask;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleModel;

import javax.ws.rs.core.MultivaluedMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:vrockai@redhat.com">Viliam Rockai</a>
 */
public class OAuthGrantBean {

    private List<RoleModel> realmRolesRequested;
    private MultivaluedMap<String, RoleModel> resourceRolesRequested;
    private String code;
    private ClientModel client;
    private List<String> claimsRequested;

    public OAuthGrantBean(String code, ClientModel client, List<RoleModel> realmRolesRequested, MultivaluedMap<String, RoleModel> resourceRolesRequested) {
        this.code = code;
        this.client = client;
        this.realmRolesRequested = realmRolesRequested;
        this.resourceRolesRequested = resourceRolesRequested;

        // todo support locale
        List<String> claims = new LinkedList<String>();
        long mask = client.getAllowedClaimsMask();
        if (ClaimMask.hasEmail(mask)) {
            claims.add("email");
        }
        if (ClaimMask.hasUsername(mask)) {
            claims.add("username");
        }
        if (ClaimMask.hasName(mask)) {
            claims.add("name");
        }
        if (ClaimMask.hasGender(mask)) {
            claims.add("gender");
        }
        if (ClaimMask.hasAddress(mask)) {
            claims.add("address");
        }
        if (ClaimMask.hasPhone(mask)) {
            claims.add("phone");
        }
        if (ClaimMask.hasPicture(mask)) {
            claims.add("picture");
        }
        if (ClaimMask.hasProfile(mask)) {
            claims.add("profile page");
        }
        if (ClaimMask.hasLocale(mask)) {
            claims.add("locale");
        }
        if (ClaimMask.hasWebsite(mask)) {
            claims.add("website");
        }
        if (claims.size() > 0) this.claimsRequested = claims;
    }

    public String getCode() {
        return code;
    }

    public MultivaluedMap<String, RoleModel> getResourceRolesRequested() {
        return resourceRolesRequested;
    }

    public List<RoleModel> getRealmRolesRequested() {
        return realmRolesRequested;
    }

    public String getClient() {
        return client.getClientId();
    }

    public List<String> getClaimsRequested() {
        return claimsRequested;
    }
}
