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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.common.util.Base64;
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
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface Secrets {
    public final String SERVICE_ACCOUNT_TOKEN="kubernetes.io/service-account-token";

   class SecretRepresentation extends BaseRepresentation {
        @JsonProperty("metadata")
        protected MetadataRepresentation metadata = new MetadataRepresentation();

        @JsonProperty("type")
        protected String type;

       public String getType() {
           return type;
       }

       public void setType(String type) {
           this.type = type;
       }

       @JsonIgnore
       public boolean isServiceAccountToken() {
           return SERVICE_ACCOUNT_TOKEN.equals(type);
       }

       @JsonIgnore
       public String getToken() {
           Object data = otherClaims.get("data");
           if (data == null) return null;
           Map<String, Object> dataMap = (Map<String, Object>)data;
           Object token = dataMap.get("token");
           if (token == null) return null;
           try {
               byte[] decoded = Base64.decode((String)token);
               return new String(decoded, "UTF-8");
           } catch (IOException e) {
               throw new RuntimeException(e);
           }
       }

   }

    @GET
    @Path("{secret}")
    @Produces(MediaType.APPLICATION_JSON)
    public SecretRepresentation get(@PathParam("secret") String secret);

    @GET
    @Path("{secret}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getPretty(@PathParam("secret") String secret, @QueryParam("pretty") boolean pretty);

}