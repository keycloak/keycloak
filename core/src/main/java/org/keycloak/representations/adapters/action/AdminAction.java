/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.representations.adapters.action;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.keycloak.Token;
import org.keycloak.TokenCategory;
import org.keycloak.common.util.Time;

/**
 * Posted to managed client from admin server.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AdminAction implements Token {
    protected String id;
    protected int expiration;
    protected String resource;
    protected String action;

    public AdminAction() {
    }

    public AdminAction(String id, int expiration, String resource, String action) {
        this.id = id;
        this.expiration = expiration;
        this.resource = resource;
        this.action = action;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonIgnore
    public boolean isExpired() {
        return Time.currentTime() > expiration;
    }

    /**
     * Time in seconds since epoc
     *
     * @return
     */
    public int getExpiration() {
        return expiration;
    }

    public void setExpiration(int expiration) {
        this.expiration = expiration;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public abstract boolean validate();

    @Override
    public TokenCategory getCategory() {
        return TokenCategory.ADMIN;
    }
}
