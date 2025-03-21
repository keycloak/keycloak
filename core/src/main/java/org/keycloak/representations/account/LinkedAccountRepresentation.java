/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.representations.account;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 *
 * @author Stan Silvert
 */
public class LinkedAccountRepresentation implements Comparable<LinkedAccountRepresentation> {
    private boolean connected;
    private boolean isSocial;
    private String providerAlias;
    private String providerName;
    private String displayName;
    private String linkedUsername;

    @JsonIgnore
    private String guiOrder;

    public String getLinkedUsername() {
        return linkedUsername;
    }

    public void setLinkedUsername(String userName) {
        this.linkedUsername = userName;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isSocial() {
        return this.isSocial;
    }

    public void setSocial(boolean isSocial) {
        this.isSocial = isSocial;
    }

    public String getProviderAlias() {
        return providerAlias;
    }

    public void setProviderAlias(String providerAlias) {
        this.providerAlias = providerAlias;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getGuiOrder() {
        return guiOrder;
    }

    public void setGuiOrder(String guiOrder) {
        this.guiOrder = guiOrder;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public int compareTo(LinkedAccountRepresentation rep) {
        if (this.getGuiOrder() == null) return 1;
        if (rep.getGuiOrder() == null) return -1;

        return Integer.valueOf(this.getGuiOrder()).compareTo(Integer.valueOf(rep.getGuiOrder()));
    }

}
