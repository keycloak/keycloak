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
package org.keycloak.protocol.openshift.connections.rest.api.v1;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.protocol.openshift.connections.rest.BaseRepresentation;
import org.keycloak.protocol.openshift.connections.rest.MetadataRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ServiceAccounts {

    String SERVICEACCOUNTS_OPENSHIFT_IO_OAUTH_REDIRECTURI = "serviceaccounts.openshift.io/oauth-redirecturi";
    String SERVICEACCOUNTS_OPENSHIFT_IO_OAUTH_WANT_CHALLENGES = "serviceaccounts.openshift.io/oauth-want-challenges";

    class ServiceAccountRepresentation extends BaseRepresentation {

        public ServiceAccountRepresentation() {
            this.kind = "ServiceAccount";
            this.apiVersion = "v1";
        }



        @JsonProperty("metadata")
        protected MetadataRepresentation metadata = new MetadataRepresentation();


        public MetadataRepresentation getMetadata() {
            return metadata;
        }

        public void setMetadata(MetadataRepresentation metadata) {
            this.metadata = metadata;
        }


        @JsonProperty("secrets")
        protected List<Map<String, String>> secrets;

        @JsonIgnore
        public Set<String> getSecrets() {
            if (secrets == null || secrets.isEmpty()) return Collections.EMPTY_SET;
            Set<String> set = new HashSet<>();
            for (Map<String, String> secret : secrets) {
                set.add(secret.values().iterator().next());
            }
            return set;
        }


        @JsonIgnore
        public String getName() {
            return metadata.getName();
        }

        @JsonIgnore
        public void setName(String name) {
            metadata.setName(name);
        }

        @JsonIgnore
        public String getNamespace() {
            return metadata.getNamespace();
        }

        @JsonIgnore
        public void setNamespace(String name) {
            metadata.setNamespace(name);
        }

        @JsonIgnore
        public Map<String, String> getAnnotations() {
            return metadata.getAnnotations();
        }

        @JsonIgnore
        public Set<String> getOauthRedirectUris() {
            Set<String> redirects = new HashSet<>();
            for (String key : getAnnotations().keySet()) {
                if (key.startsWith(SERVICEACCOUNTS_OPENSHIFT_IO_OAUTH_REDIRECTURI)) {
                    redirects.add(getAnnotations().get(key));
                }
            }
            return redirects;
        }

        @JsonIgnore
        public void addRedirectUri(String uri) {
            for (int i = 0; i < 100; i++) {
                String key = SERVICEACCOUNTS_OPENSHIFT_IO_OAUTH_REDIRECTURI + "." + (i + 1);
                if (getAnnotations().containsKey(key)) continue;
                getAnnotations().put(key, uri);
                break;
            }
        }


        @JsonIgnore
        public boolean oauthWantChallenges() {
            String val = getAnnotations().get(SERVICEACCOUNTS_OPENSHIFT_IO_OAUTH_WANT_CHALLENGES);
            if (val == null) return false;
            return val.trim().equalsIgnoreCase("true");
        }

        @JsonIgnore
        public void setOauthWantChallenges(boolean wantChallenges) {
            getAnnotations().put(SERVICEACCOUNTS_OPENSHIFT_IO_OAUTH_WANT_CHALLENGES, Boolean.toString(wantChallenges));

        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ServiceAccountRepresentation create(ServiceAccountRepresentation rep);

    @GET
    @Path("{sa-name}")
    @Produces(MediaType.APPLICATION_JSON)
    public ServiceAccountRepresentation get(@PathParam("sa-name") String name);

    @DELETE
    @Path("{sa-name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("sa-name") String name);

}