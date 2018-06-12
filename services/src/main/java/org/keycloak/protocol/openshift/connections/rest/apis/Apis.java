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
package org.keycloak.protocol.openshift.connections.rest.apis;

import org.keycloak.protocol.openshift.connections.rest.apis.kubernetes.KubernetesAuthentication;
import org.keycloak.protocol.openshift.connections.rest.apis.kubernetes.TokenReview;
import org.keycloak.protocol.openshift.connections.rest.apis.oauth.OpenshiftOAuth;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface Apis {

    @Path("authentication.k8s.io/v1beta1")
    KubernetesAuthentication kubernetesAuthentication();

    @Path("oauth.openshift.io/v1")
    OpenshiftOAuth oauth();

}
