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

package org.keycloak.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Specifies a mapping from user data to a protocol claim assertion.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ProtocolMapperModel implements Serializable {

    protected String id;
    protected String name;
    protected String protocol;
    protected String protocolMapper;
    protected boolean consentRequired;
    protected String consentText;
    protected Map<String, String> config;

    public ProtocolMapperModel() {

    }

    public ProtocolMapperModel(ProtocolMapperModel model) {
        this.id = model.id;
        this.name = model.name;
        this.protocol = model.protocol;
        this.protocolMapper = model.protocolMapper;
        this.consentRequired = model.consentRequired;
        this.consentText = model.consentText;
        if (model.config != null) {
            this.config = new HashMap<>(model.config);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getProtocolMapper() {
        return protocolMapper;
    }

    public void setProtocolMapper(String protocolMapper) {
        this.protocolMapper = protocolMapper;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ProtocolMapperModel other = (ProtocolMapperModel) obj;
        if (this.consentRequired != other.consentRequired) {
            return false;
        }
        if ( ! Objects.equals(this.id, other.id)) {
            return false;
        }
        if ( ! Objects.equals(this.name, other.name)) {
            return false;
        }
        if ( ! Objects.equals(this.protocol, other.protocol)) {
            return false;
        }
        if ( ! Objects.equals(this.protocolMapper, other.protocolMapper)) {
            return false;
        }
        if ( ! Objects.equals(this.consentText, other.consentText)) {
            return false;
        }
        if ( ! Objects.equals(this.config, other.config)) {
            return false;
        }
        return true;
    }


    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
