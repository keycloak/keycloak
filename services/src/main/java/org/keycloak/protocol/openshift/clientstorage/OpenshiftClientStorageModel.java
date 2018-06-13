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
package org.keycloak.protocol.openshift.clientstorage;

import org.keycloak.component.ComponentModel;
import org.keycloak.storage.client.ClientStorageProviderModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OpenshiftClientStorageModel extends ClientStorageProviderModel {
    protected transient String token;
    protected transient String openshiftUri;

    public OpenshiftClientStorageModel() {
    }

    public OpenshiftClientStorageModel(ComponentModel copy) {
        super(copy);
    }

    public String getToken() {
        if (token == null) {
            token = getConfig().getFirst(OpenshiftClientStorageProviderFactory.ACCESS_TOKEN);
        }
        return token;
    }

    public void setToken(String token) {
        this.token = token;
        getConfig().putSingle(OpenshiftClientStorageProviderFactory.ACCESS_TOKEN, token);
    }

    public String getOpenshiftUri() {
        if (openshiftUri == null) {
            openshiftUri = getConfig().getFirst(OpenshiftClientStorageProviderFactory.OPENSHIFT_URI);
        }
        return openshiftUri;
    }

    public void setOpenshiftUri(String openshiftUri) {
        this.openshiftUri = openshiftUri;
        getConfig().putSingle(OpenshiftClientStorageProviderFactory.OPENSHIFT_URI, openshiftUri);
    }
}
