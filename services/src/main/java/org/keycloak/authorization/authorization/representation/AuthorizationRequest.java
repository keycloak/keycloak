/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authorization.authorization.representation;

import org.keycloak.representations.idm.authorization.PermissionTicketToken;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AuthorizationRequest {

    private String ticket;
    private String rpt;
    private String claimToken;
    private String claimTokenFormat;
    private String pct;
    private String scope;
    private PermissionTicketToken permissions;
    private AuthorizationRequestMetadata metadata;
    private String audience;
    private String accessToken;

    public AuthorizationRequest(String ticket) {
        this.ticket = ticket;
    }

    public AuthorizationRequest() {
        this(null);
    }

    public String getTicket() {
        return this.ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public String getRpt() {
        return this.rpt;
    }

    public void setRpt(String rpt) {
        this.rpt = rpt;
    }

    public void setClaimToken(String claimToken) {
        this.claimToken = claimToken;
    }

    public String getClaimToken() {
        return claimToken;
    }

    public void setClaimTokenFormat(String claimTokenFormat) {
        this.claimTokenFormat = claimTokenFormat;
    }

    public String getClaimTokenFormat() {
        return claimTokenFormat;
    }

    public void setPct(String pct) {
        this.pct = pct;
    }

    private String getPct() {
        return pct;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getScope() {
        return scope;
    }

    public void setPermissions(PermissionTicketToken permissions) {
        this.permissions = permissions;
    }

    public PermissionTicketToken getPermissions() {
        return permissions;
    }

    public AuthorizationRequestMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(AuthorizationRequestMetadata metadata) {
        this.metadata = metadata;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getAudience() {
        return audience;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }
}
