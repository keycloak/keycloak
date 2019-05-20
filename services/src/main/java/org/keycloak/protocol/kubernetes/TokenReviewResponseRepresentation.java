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

package org.keycloak.protocol.kubernetes;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class TokenReviewResponseRepresentation implements Serializable {
    @JsonProperty("apiVersion")
    protected String apiVersion = "authentication.k8s.io/v1beta1";
    @JsonProperty("kind")
    protected String kind = "TokenReview";

    protected Map<String, Object> otherClaims = new HashMap<>();

    public static class Status implements Serializable {
        @JsonProperty("authenticated")
        protected boolean authenticated;

        protected Map<String, Object> otherClaims = new HashMap<>();

        @JsonAnyGetter
        public Map<String, Object> getOtherClaims() {
            return otherClaims;
        }

        @JsonAnySetter
        public void setOtherClaims(String name, Object value) {
            otherClaims.put(name, value);
        }

        public static class User implements Serializable {
            @JsonProperty("username")
            protected String username;
            @JsonProperty("uid")
            protected String uid;

            @JsonProperty("groups")
            protected Set<String> groups = new HashSet<>();

            public static class Extra implements Serializable {
                protected Map<String, Object> data = new HashMap<>();

                @JsonAnyGetter
                public Map<String, Object> getData() {
                    return data;
                }

                @JsonAnySetter
                public void setData(String name, Object value) {
                    data.put(name, value);
                }

            }

            @JsonProperty("extra")
            protected Extra extra;

            public String getUsername() {
                return username;
            }

            public void setUsername(String username) {
                this.username = username;
            }

            public String getUid() {
                return uid;
            }

            public void setUid(String uid) {
                this.uid = uid;
            }

            public Set<String> getGroups() {
                return groups;
            }

            public void setGroups(Set<String> groups) {
                this.groups = groups;
            }

            public Extra getExtra() {
                return extra;
            }

            public void setExtra(Extra extra) {
                this.extra = extra;
            }

            public void putExtra(String key, Object value) {
                if (extra == null) extra = new Extra();
                extra.data.put(key, value);
            }
        }

        @JsonProperty("user")
        protected User user;

        @JsonProperty("error")
        protected String error;

        public boolean isAuthenticated() {
            return authenticated;
        }

        public void setAuthenticated(boolean authenticated) {
            this.authenticated = authenticated;
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    @JsonProperty("status")
    protected Status status = new Status();


    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

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

    public static TokenReviewResponseRepresentation error(String error) {
        TokenReviewResponseRepresentation response = new TokenReviewResponseRepresentation();
        response.getStatus().setError(error);
        return response;
    }
    public static TokenReviewResponseRepresentation success() {
        TokenReviewResponseRepresentation response = new TokenReviewResponseRepresentation();
        response.getStatus().setAuthenticated(true);
        response.getStatus().setUser(new Status.User());
        return response;
    }
}
