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

package org.keycloak.jose.jws;

import org.keycloak.jose.JOSEHeader;
import org.keycloak.jose.jwk.JWK;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JWSHeader implements JOSEHeader {
    @JsonProperty("alg")
    private Algorithm algorithm;

    @JsonProperty("typ")
    private String type;

    @JsonProperty("cty")
    private String contentType;

    @JsonProperty("kid")
    private String keyId;

    @JsonProperty("jwk")
    private JWK key;

    @JsonProperty("x5c")
    private List<String> x5c;

    public JWSHeader() {
    }

    public JWSHeader(Algorithm algorithm, String type, String contentType) {
        this.algorithm = algorithm;
        this.type = type;
        this.contentType = contentType;
    }

    public JWSHeader(Algorithm algorithm, String type, String keyId, JWK key) {
        this.algorithm = algorithm;
        this.type = type;
        this.keyId = keyId;
        this.key = key;
    }

    public Algorithm getAlgorithm() {
        return algorithm;
    }

    @JsonIgnore
    @Override
    public String getRawAlgorithm() {
        return getAlgorithm().name();
    }

    public String getType() {
        return type;
    }

    public String getContentType() {
        return contentType;
    }

    public String getKeyId() {
        return keyId;
    }

    public JWK getKey() {
        return key;
    }

    public List<String> getX5c() {
        return x5c;
    }

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    }

    public String toString() {
        try {
            return mapper.writeValueAsString(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

}
