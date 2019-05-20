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

package org.keycloak.protocol.openshift;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.keycloak.common.util.Time;
import org.keycloak.json.StringOrArrayDeserializer;
import org.keycloak.json.StringOrArraySerializer;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class TokenReviewRequestRepresentation implements Serializable {
    public static TokenReviewRequestRepresentation create(String token) {
        TokenReviewRequestRepresentation rep = new TokenReviewRequestRepresentation();
        rep.setSpec(new Spec());
        rep.getSpec().setToken(token);
        return rep;
    }
    @JsonProperty("apiVersion")
    protected String apiVersion = "authentication.k8s.io/v1beta1";
    @JsonProperty("kind")
    protected String kind = "TokenReview";

    protected Map<String, Object> otherClaims = new HashMap<>();

    public static class Spec implements Serializable {
        @JsonProperty("token")
        protected String token;
        protected Map<String, Object> otherClaims = new HashMap<>();

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        /**
         * This is a map of any other claims and data that might be in the request.  Don't know what Openshift will add in future
         *
         * @return
         */
        @JsonAnyGetter
        public Map<String, Object> getOtherClaims() {
            return otherClaims;
        }

        @JsonAnySetter
        public void setOtherClaims(String name, Object value) {
            otherClaims.put(name, value);
        }
    }

    public static TokenReviewRequestRepresentation build(String token) {
        TokenReviewRequestRepresentation request = new TokenReviewRequestRepresentation();
        request.setSpec(new Spec());
        request.getSpec().setToken(token);
        return request;
    }

    @JsonProperty("spec")
    protected Spec spec;

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Spec getSpec() {
        return spec;
    }

    public void setSpec(Spec spec) {
        this.spec = spec;
    }

    /**
     * This is a map of any other claims and data that might be in the request.  Don't know what Openshift will add in future
     *
     * @return
     */
    @JsonAnyGetter
    public Map<String, Object> getOtherClaims() {
        return otherClaims;
    }

    @JsonAnySetter
    public void setOtherClaims(String name, Object value) {
        otherClaims.put(name, value);
    }
}
