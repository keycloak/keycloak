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
package org.keycloak.protocol.openshift.connections.rest.apis.oauth;

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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface OAuthClients {

    class OAuthClientRepresentation extends BaseRepresentation {
        public static OAuthClientRepresentation create() {
            OAuthClientRepresentation rep = new OAuthClientRepresentation();
            rep.wasScopeRestrictionsParsed = true;
            return rep;
        }

        protected OAuthClientRepresentation() {
        }

        @JsonProperty("metadata")
        protected MetadataRepresentation metadata = new MetadataRepresentation();


        public MetadataRepresentation getMetadata() {
            return metadata;
        }

        public void setMetadata(MetadataRepresentation metadata) {
            this.metadata = metadata;
        }

        @JsonIgnore
        public String getName() {
            return metadata.getName();
        }

        @JsonIgnore
        public void setName(String name) {
            metadata.setName(name);
        }

        @JsonProperty("respondWithChallenges")
        protected boolean respondWithChallenges;

        @JsonProperty("redirectURIs")
        protected Set<String> redirectURIs;

        @JsonProperty("grantMethod")
        protected String grantMethod;

        @JsonProperty("secret")
        protected String secret;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public boolean isRespondWithChallenges() {
            return respondWithChallenges;
        }

        public void setRespondWithChallenges(boolean respondWithChallenges) {
            this.respondWithChallenges = respondWithChallenges;
        }

        public Set<String> getRedirectURIs() {
            return redirectURIs;
        }

        public void setRedirectURIs(Set<String> redirectURIs) {
            this.redirectURIs = redirectURIs;
        }

        public void addRedirectURI(String redirect) {
            if (this.redirectURIs == null) this.redirectURIs = new HashSet<>();
            this.redirectURIs.add(redirect);
        }

        public String getGrantMethod() {
            return grantMethod;
        }

        public void setGrantMethod(String grantMethod) {
            this.grantMethod = grantMethod;
        }

        @JsonProperty("scopeRestrictions")
        protected List<Object> scopeRestrictions;

        @JsonIgnore
        protected boolean wasScopeRestrictionsParsed;

        @JsonIgnore
        protected Set<String> literalScopeRestrictions;


        @JsonIgnore
        protected List<ClusterRoleRestriction> clusterRoleRestrictions;

        @JsonIgnore
        public Set<String> getLiteralScopeRestrictions() {
            parseScopeRestrictions();
            return literalScopeRestrictions;
        }

        @JsonIgnore
        public List<ClusterRoleRestriction> getClusterRoleRestrictions() {
            parseScopeRestrictions();
            return clusterRoleRestrictions;
        }

        protected void parseScopeRestrictions() {
            if (wasScopeRestrictionsParsed) return;
            wasScopeRestrictionsParsed = true;
            if (scopeRestrictions == null) return;
            List<Object> marshalledScope = scopeRestrictions;
            scopeRestrictions = new LinkedList<>();
            for (Object obj : marshalledScope) {
                Map<String, Object> data = (Map<String, Object>)obj;
                if (data.containsKey("literals")) {
                    if (literalScopeRestrictions == null) {
                        literalScopeRestrictions = new HashSet<>();
                        Map<String, Object> map = new HashMap<>();
                        map.put("literals", literalScopeRestrictions);
                        scopeRestrictions.add(map);
                    }
                    Collection<String> scopes = (Collection<String>)data.get("literals");
                    literalScopeRestrictions.addAll(scopes);
                } else if (data.containsKey("clusterRole")) {
                    ClusterRoleRestriction roleRestriction = new ClusterRoleRestriction();
                    Map<String, Object> roleData = (Map<String, Object>)data.get("clusterRole");
                    Collection<String> namespaces = (Collection<String>)roleData.get("namespaces");
                    Collection<String> roleNames = (Collection<String>)roleData.get("roleNames");
                    boolean allowEscalation = roleData.containsKey("allowEscalation") ? (Boolean)roleData.get("allowEscalation") : false;
                    roleRestriction.namespaces.addAll(namespaces);
                    roleRestriction.roleNames.addAll(roleNames);
                    roleRestriction.allowEscalation = allowEscalation;
                    if (clusterRoleRestrictions == null) {
                        clusterRoleRestrictions = new LinkedList<>();
                    }
                    clusterRoleRestrictions.add(roleRestriction);
                    Map<String, Object> map = new HashMap<>();
                    map.put("clusterRole", roleRestriction);
                    scopeRestrictions.add(map);
                }

            }
        }


        public void addLiteralScopeRestriction(String scope) {
            if (literalScopeRestrictions == null) {
                parseScopeRestrictions();
                if (scopeRestrictions == null) {
                    scopeRestrictions = new LinkedList<>();
                }
                literalScopeRestrictions = new HashSet<>();
                Map<String, Object> map = new HashMap<>();
                map.put("literals", literalScopeRestrictions);
                scopeRestrictions.add(map);
            }
            literalScopeRestrictions.add(scope);
        }

        public void addClusterRoleScopeRestriction(ClusterRoleRestriction restriction) {
            parseScopeRestrictions();
            if (clusterRoleRestrictions == null) {
                clusterRoleRestrictions = new LinkedList<>();
            }
            clusterRoleRestrictions.add(restriction);
            if (scopeRestrictions == null) {
                scopeRestrictions = new LinkedList<>();
            }
            Map<String, Object> map = new HashMap<>();
            map.put("clusterRole", restriction);
            scopeRestrictions.add(map);
        }

        private static class LiteralScopeRestriction {
            @JsonProperty("literals")
            protected Set<String> literals = new HashSet<>();

            public Set<String> getLiterals() {
                return literals;
            }

            public void setLiterals(Set<String> literals) {
                this.literals = literals;
            }
        }

        public static class ClusterRoleRestriction {

            @JsonProperty("allowEscalation")
            protected boolean allowEscalation;
            @JsonProperty("namespaces")
            protected Set<String> namespaces = new HashSet<>();
            @JsonProperty("roleNames")
            protected Set<String> roleNames = new HashSet<>();

            public boolean isAllowEscalation() {
                return allowEscalation;
            }

            public void setAllowEscalation(boolean allowedEscalation) {
                this.allowEscalation = allowedEscalation;
            }

            public Set<String> getNamespaces() {
                return namespaces;
            }

            public void setNamespaces(Set<String> namespaces) {
                this.namespaces = namespaces;
            }

            public Set<String> getRoleNames() {
                return roleNames;
            }

            public void setRoleNames(Set<String> roleNames) {
                this.roleNames = roleNames;
            }
        }

    }
    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    OAuthClientRepresentation get(@PathParam("name") String name);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Response create(OAuthClientRepresentation client);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    Response delete(@PathParam("name") String name);


}
