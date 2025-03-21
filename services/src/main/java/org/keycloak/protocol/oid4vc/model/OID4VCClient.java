/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oid4vc.model;

import java.util.Collections;
import java.util.List;

/**
 * Pojo, containing all information required to create a VCClient.
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCClient {

    /**
     * Id of the client.
     */
    private String id;

    /**
     * Did of the target/client, will be used as client-id
     */
    private String clientDid;
    /**
     * Comma-separated list of supported credentials types
     */
    private List<SupportedCredentialConfiguration> supportedVCTypes;
    /**
     * Description of the client, will f.e. be displayed in the admin-console
     */
    private String description;
    /**
     * Human-readable name of the client
     */
    private String name;

    public OID4VCClient() {
    }

    public OID4VCClient(String id, String clientDid, List<SupportedCredentialConfiguration> supportedVCTypes, String description, String name) {
        this.id = id;
        this.clientDid = clientDid;
        this.supportedVCTypes = supportedVCTypes;
        this.description = description;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public OID4VCClient setId(String id) {
        this.id = id;
        return this;
    }

    public String getClientDid() {
        return clientDid;
    }

    public OID4VCClient setClientDid(String clientDid) {
        this.clientDid = clientDid;
        return this;
    }

    public List<SupportedCredentialConfiguration> getSupportedVCTypes() {
        return supportedVCTypes;
    }

    public OID4VCClient setSupportedVCTypes(List<SupportedCredentialConfiguration> supportedVCTypes) {
        this.supportedVCTypes = Collections.unmodifiableList(supportedVCTypes);
        return this;
    }

    public String getDescription() {
        return description;
    }

    public OID4VCClient setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getName() {
        return name;
    }

    public OID4VCClient setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OID4VCClient that)) return false;

        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
        if (getClientDid() != null ? !getClientDid().equals(that.getClientDid()) : that.getClientDid() != null)
            return false;
        if (getSupportedVCTypes() != null ? !getSupportedVCTypes().equals(that.getSupportedVCTypes()) : that.getSupportedVCTypes() != null)
            return false;
        if (getDescription() != null ? !getDescription().equals(that.getDescription()) : that.getDescription() != null)
            return false;
        return getName() != null ? getName().equals(that.getName()) : that.getName() == null;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getClientDid() != null ? getClientDid().hashCode() : 0);
        result = 31 * result + (getSupportedVCTypes() != null ? getSupportedVCTypes().hashCode() : 0);
        result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        return result;
    }
}